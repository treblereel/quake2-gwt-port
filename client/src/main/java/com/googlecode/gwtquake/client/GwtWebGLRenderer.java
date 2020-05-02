/*
Copyright (C) 2010 Copyright 2010 Google Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package com.googlecode.gwtquake.client;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.googlecode.gwtquake.shared.client.Renderer;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.ResourceLoader;
import com.googlecode.gwtquake.shared.render.GlRenderer;
import com.googlecode.gwtquake.shared.render.GlState;
import com.googlecode.gwtquake.shared.render.Image;
import com.googlecode.gwtquake.shared.render.Images;
import com.googlecode.gwtquake.shared.sys.KBD;
import elemental2.core.Uint8ClampedArray;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLCanvasElement;
import elemental2.dom.HTMLImageElement;
import elemental2.dom.HTMLVideoElement;
import elemental2.webgl.WebGLRenderingContext;
import jsinterop.base.Js;
import org.gwtproject.timer.client.Timer;

import static elemental2.webgl.WebGLRenderingContext.LINEAR;
import static elemental2.webgl.WebGLRenderingContext.LINEAR_MIPMAP_NEAREST;
import static elemental2.webgl.WebGLRenderingContext.RGBA;
import static elemental2.webgl.WebGLRenderingContext.TEXTURE_2D;
import static elemental2.webgl.WebGLRenderingContext.TEXTURE_MAG_FILTER;
import static elemental2.webgl.WebGLRenderingContext.TEXTURE_MIN_FILTER;
import static elemental2.webgl.WebGLRenderingContext.UNSIGNED_BYTE;

public class GwtWebGLRenderer extends GlRenderer implements Renderer {

    static final int IMAGE_CHECK_TIME = 250;
    static final int MAX_IMAGE_REQUEST_COUNT = 12;
    static int HOLODECK_TEXTURE_SIZE = 128;
    static int MASK = 15;
    static int HIT = MASK / 2;
    KBD kbd = new GwtKBD();
    ByteBuffer holoDeckTexture = ByteBuffer.allocateDirect(HOLODECK_TEXTURE_SIZE *
                                                                   HOLODECK_TEXTURE_SIZE * 4);
    WebGLGl1Contect webGL;
    HTMLCanvasElement canvas1;
    HTMLCanvasElement canvas2;
    ArrayList<Image> imageQueue = new ArrayList<>();
    int waitingForImages;
    HTMLVideoElement video;
    HTMLCanvasElement canvas;
    boolean videoVisible = false;

    public GwtWebGLRenderer(HTMLCanvasElement canvas, HTMLVideoElement video) {
        super(canvas.width, canvas.height);
        GlState.gl = this.webGL = new WebGLGl1Contect(canvas);
        this.canvas = canvas;
        this.video = video;

        for (int y = 0; y < HOLODECK_TEXTURE_SIZE; y++) {
            for (int x = 0; x < HOLODECK_TEXTURE_SIZE; x++) {
                holoDeckTexture.put((byte) 0);
                holoDeckTexture.put((byte) (((x & MASK) == HIT) || ((y & MASK) == HIT) ? 255 : 0));
                holoDeckTexture.put((byte) 0);
                holoDeckTexture.put((byte) 0xff);
            }
        }
        holoDeckTexture.rewind();

        canvas1 = (HTMLCanvasElement) DomGlobal.document.createElement("canvas");
        canvas1.style.display = "NONE";
        canvas1.width = 128;
        canvas1.height = 128;
        DomGlobal.document.body.appendChild(canvas1);

        canvas2 = (HTMLCanvasElement) DomGlobal.document.createElement("canvas");
        canvas2.width = 128;
        canvas2.height = 128;
        canvas2.style.display = "NONE";
        DomGlobal.document.body.appendChild(canvas2);

        init();
    }

    private static String convertPicName(String name, int type) {
        int dotIdx = name.indexOf('.');
        assert dotIdx != -1;
        return "baseq2/" + name.substring(0, dotIdx) + ".png";
    }

    public KBD getKeyboardHandler() {
        return kbd;
    }

    @Override
    public void GL_ResampleTexture(int[] in, int inwidth, int inheight,
                                   int[] out, int outwidth, int outheight) {

        if (canvas1.width < inwidth) {
            canvas1.width = inwidth;
        }
        if (canvas1.height < inheight) {
            canvas1.height = inheight;
        }

        elemental2.dom.CanvasRenderingContext2D inCtx = Js.uncheckedCast(canvas1.getContext("2d"));
        elemental2.dom.ImageData data = inCtx.createImageData(inwidth, inheight);
        Uint8ClampedArray pixels = data.data;

        int len = inwidth * inheight;
        int p = 0;

        for (int i = 0; i < len; i++) {
            int abgr = in[i];
            pixels.setAt(p, Double.valueOf(abgr & 255));
            pixels.setAt(p + 1, Double.valueOf((abgr >> 8) & 255));
            pixels.setAt(p + 2, Double.valueOf((abgr >> 16) & 255));
            pixels.setAt(p + 3, Double.valueOf((abgr >> 24) & 255));
            p += 4;
        }
        inCtx.putImageData(data, 0, 0);

        if (canvas2.width < outwidth) {
            canvas2.width = outwidth;
        }
        if (canvas2.height < outheight) {
            canvas2.height = outheight;
        }

        elemental2.dom.CanvasRenderingContext2D outCtx = Js.uncheckedCast(canvas2.getContext("2d"));
        outCtx.drawImage(canvas1, 0, 0, inwidth, inheight, 0, 0, outwidth, outheight);

        data = outCtx.getImageData(0, 0, outwidth, outheight);
        pixels = data.data;

        len = outwidth * outheight;
        p = 0;

        for (int i = 0; i < len; i++) {
            int r = pixels.getAt(p).intValue() & 255;
            int g = pixels.getAt(p + 1).intValue() & 255;
            int b = pixels.getAt(p + 2).intValue() & 255;
            int a = pixels.getAt(p + 3).intValue() & 255;
            p += 4;
            out[i] = (a << 24) | (b << 16) | (g << 8) | r;
        }
    }

    public Image GL_LoadNewImage(final String name, int type) {
        final Image image = Images.GL_Find_free_image_t(name, type);

        int cut = name.lastIndexOf('.');
        String normalizedName = cut == -1 ? name : name.substring(0, cut);
        int[] d = getImageSize(normalizedName);
        if (d == null) {
            //GlState.gl.log("Size not found for " + name);
            image.width = 128;
            image.height = 128;
        } else {
            image.width = d[0];
            image.height = d[1];
        }

        if (type != com.googlecode.gwtquake.shared.common.QuakeImage.it_pic) {
            GlState.gl.glTexImage2D((int) TEXTURE_2D, 0, (int) RGBA, (int) HOLODECK_TEXTURE_SIZE, (int) HOLODECK_TEXTURE_SIZE, 0, (int) RGBA,
                                    (int) UNSIGNED_BYTE, holoDeckTexture);
            GlState.gl.glTexParameterf((int) TEXTURE_2D, (int) TEXTURE_MIN_FILTER, (int) LINEAR);
            GlState.gl.glTexParameterf((int) TEXTURE_2D, (int) TEXTURE_MAG_FILTER, (int) LINEAR);
        }

        imageQueue.add(image);
        if (imageQueue.size() == 1) {
            new ImageLoader().schedule();
        }

        return image;
    }

    private int[] getImageSize(String name) {
        return Js.uncheckedCast(Js.asPropertyMap(Js.asPropertyMap(DomGlobal.window).get("__imageSizes")).get(name));
    }

    public void loaded(Image image, HTMLImageElement img) {
        setPicDataHighLevel(image, img);
        //	setPicDataLowLevel(image, img);
    }

/*  public void __setPicDataHighLevel(Image image, ImageElement img) {
    image.has_alpha = true;
    image.complete = true;
    image.height = img.getHeight();
    image.width = img.getWidth();
    image.upload_height = image.height;
    image.upload_width = image.width;
    Images.GL_Bind(image.texnum);
    webGL.glTexImage2d(TEXTURE_2D, 0, RGBA, RGBA, UNSIGNED_BYTE, img);
    GlState.gl.glTexParameterf(TEXTURE_2D, TEXTURE_MIN_FILTER, LINEAR);
    GlState.gl.glTexParameterf(TEXTURE_2D, TEXTURE_MAG_FILTER, LINEAR);
  }*/

/*	public void setPicDataLowLevel(Image image, ImageElement img) {
		CanvasElement canvas = (CanvasElement) Document.get().createElement("canvas");
		int w = img.getWidth();
		int h = img.getHeight();
		canvas.setWidth(w);
		canvas.setHeight(h);
//		canvas.getStyle().setProperty("border", "solid 1px green");
		canvas.getStyle().setDisplay(Display.NONE);
		Document.get().getBody().appendChild(canvas);
		CanvasRenderingContext2D ctx = canvas.getContext2D();
		ctx.drawImage(img, 0, 0);
		ImageData data = ctx.getImageData(0, 0, w, h);
		CanvasPixelArray pixels = data.getData();
		
		int count = w * h * 4;
		byte[] pic = new byte[count];
		for (int i = 0; i < count; i += 4) {
			pic[i + 3] = (byte) pixels.get(i + 3); // alpha, then bgr
			pic[i + 2] = (byte) pixels.get(i + 2);
			pic[i + 1] = (byte) pixels.get(i + 1);
			pic[i] = (byte) pixels.get(i);
		}
		
		image.setData(pic, w, h, 32);
	}
	
	 protected void debugLightmap(IntBuffer lightmapBuffer, int w, int h, float scale) {
		 CanvasElement canvas = (CanvasElement) Document.get().createElement("canvas");
		 canvas.setWidth(w);
		 canvas.setHeight(h);
		 Document.get().getBody().appendChild(canvas);
		 ImageData id = canvas.getContext2D().createImageData(w, h);
		 CanvasPixelArray pd = id.getData();
		 for (int i = 0; i < w*h; i++) {
			 int abgr = lightmapBuffer.get(i);
			 pd.set(i*4, abgr & 255);
			 pd.set(i*4+1, abgr & 255);
			 pd.set(i*4+2, abgr & 255);
			 pd.set(i*4+3, abgr & 255);
		 }
		 canvas.getContext2D().putImageData(id, 0, 0);
	 }*/

    public void setPicDataHighLevel(Image image, HTMLImageElement img) {
        //GlState.gl.log("setPicDataHighLevel " + image.name + " " + image.height + " " + image.width);

        image.has_alpha = true;
        image.complete = true;
        image.height = img.height;
        image.width = img.width;

        boolean mipMap = image.type != com.googlecode.gwtquake.shared.common.QuakeImage.it_pic &&
                image.type != com.googlecode.gwtquake.shared.common.QuakeImage.it_sky;

        Images.GL_Bind(image.texnum);

        int p2w = 1 << ((int) Math.ceil(Math.log(image.width) / Math.log(2)));
        int p2h = 1 << ((int) Math.ceil(Math.log(image.height) / Math.log(2)));

        if (mipMap) {
            p2w = p2h = Math.max(p2w, p2h);
        }

        image.upload_width = p2w;
        image.upload_height = p2h;

        int level = 0;
        do {
            canvas1.width = p2w;
            canvas1.height = p2h;

            Js.<elemental2.dom.CanvasRenderingContext2D>uncheckedCast(canvas1.getContext("2d")).clearRect(0, 0, p2w, p2h);
            Js.<elemental2.dom.CanvasRenderingContext2D>uncheckedCast(canvas1.getContext("2d")).drawImage(img, 0, 0, p2w, p2h);

            webGL.glTexImage2d((int) TEXTURE_2D, level++, (int) RGBA, (int) RGBA, (int) WebGLRenderingContext.UNSIGNED_BYTE, canvas1);

            p2w = p2w / 2;
            p2h = p2h / 2;
        }
        while (mipMap && p2w > 0);

        GlState.gl.glTexParameterf((int) TEXTURE_2D, (int) TEXTURE_MIN_FILTER,
                                   mipMap ? (int) LINEAR_MIPMAP_NEAREST : (int) LINEAR);
        GlState.gl.glTexParameterf((int) TEXTURE_2D, (int) TEXTURE_MAG_FILTER, (int) LINEAR);
    }

    public void CinematicSetPalette(byte[] palette) {
        setVideoVisible(palette != null);
    }

    private void setVideoVisible(boolean show) {
        if (videoVisible == show) {
            return;
        }
        DomGlobal.console.log("setVideoVisible(" + show + ")");
        videoVisible = show;
        if (show) {
            canvas.style.setProperty("display", "none");
            video.style.setProperty("display", "");
            if (video.getAttribute("src") != null && !video.ended) {
                video.muted = false;
                video.play();
            }
        } else {
            canvas.style.setProperty("display", "");
            video.style.setProperty("display", "none");
            if (video.getAttribute("src") != null && !video.ended) {
                video.pause();
            }
        }
    }

    public boolean showVideo(String name) {
        if (name == null) {
            setVideoVisible(false);
            return true;
        }

//		String src = GWT.getModuleBaseURL();
//		int cut = src.indexOf("/", 8);
//		if (cut == -1) {
//			cut = src.length();
//		}
        String src = "baseq2/video/" + name + ".mp4";

        System.out.println("trying to play video: " + src);

        video.setAttribute("class", "video-stream");
        video.setAttribute("src", src);
        if (!Double.isNaN(video.duration)) {
            video.currentTime = 0;
        }

        setVideoVisible(true);
        return true;
    }

    public boolean updateVideo() {

        return !video.ended;
    }

    class ImageLoader extends Timer {

        @Override
        public void run() {
            while (!ResourceLoader.Pump() && waitingForImages < MAX_IMAGE_REQUEST_COUNT && imageQueue.size() > 0) {
                final Image image = imageQueue.remove(0);
                final HTMLImageElement img = (HTMLImageElement) DomGlobal.document.createElement("img");

                String picUrl = convertPicName(image.name, image.type);
          /*    if (picUrl.endsWith("ggrat6_2.png")) {
                picUrl = convertPicName("textures/tron_poster.jpg", 0);
              }*/
                img.src = picUrl;
                img.style.display = "NONE";
                DomGlobal.document.body.appendChild(img);

                HTMLImageElement imageElement = (HTMLImageElement) DomGlobal.document.createElement("img");
                DomGlobal.document.body.appendChild(imageElement);
                imageElement.style.display = "NONE";
                imageElement.src = picUrl;

                imageElement.addEventListener("load", evt -> {
                    imageElement.setAttribute("complete", true);
                    waitingForImages(-1);
                    loaded(image, imageElement);
                });
                imageElement.addEventListener("error", evt -> {
                    imageElement.setAttribute("complete", true);
                    GlState.gl.log("load errors for " + imageElement.src);
                    waitingForImages(-1);
                    GlState.gl.log("1 " + image);

                    image.complete = true;

                    GlState.gl.log("2");

                });
            }
            if (imageQueue.size() > 0) {
                schedule();
            }
        }

        protected void waitingForImages(int i) {
            waitingForImages += i;
            if (waitingForImages > 0) {
                DomGlobal.console.log("Waiting for " + waitingForImages + " images\n");
                Com.Printf("Waiting for " + waitingForImages + " images\n");
            }
        }

        public void schedule() {
            schedule(IMAGE_CHECK_TIME);
        }
    }
}
