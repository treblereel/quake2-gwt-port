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
package com.googlecode.gwtquake.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.googlecode.gwtquake.server.CompatibilityImpl;
import com.googlecode.gwtquake.shared.common.Compatibility;

public class Downloader implements Runnable {

    static final String[] MIRRORS = {
            "https://ftp.gwdg.de/pub/misc/ftp.idsoftware.com/idstuff/quake2/",
            "ftp://ftp.fu-berlin.de/pc/msdos/games/idgames/idstuff/quake2/",
            "ftp://ftp.idsoftware.com/idstuff/quake2/",
            "ftp://ftp.gamers.org/pub/games/idgames2/idstuff/quake2/",
    };
    private final static String Q314 = "q2-314-demo-x86.exe";
    private final static String Q320 = "q2-3.20-x86-full-ctf.exe";
    boolean ok;
    //static final int FILE_SIZE = 39015499;
    private URL url;
    private boolean hiresPak;
    private byte[] buf = new byte[65536];
    private String current;

    private Map<String, Integer> patches = new HashMap<String, Integer>() {{
        put(Q314, 39015499);
        put(Q320, 19267584);
    }};

    public Downloader() throws InterruptedException, MalformedURLException {
        //Compatibility.impl = new CompatibilityImpl();
        if (new File("raw", "baseq2").exists()) {
            System.out.println("raw/baseq2 already exists; no need to download");
            return;
        }
        doDownload(Q314);
        doDownload(Q320);
        return;
    }

    private void doDownload(String file) throws MalformedURLException, InterruptedException {
        int m0 = selectInitialMirror();
        this.current = file;
        for (int i = 0; i < MIRRORS.length * 2; i++) {
            Downloader download = new Downloader(MIRRORS[(i + m0) % MIRRORS.length], file);
            Thread t = new Thread(download);
            t.start();
            t.join();
            if (download.ok) {
                return;
            }
        }
        throw new RuntimeException("Too many retries. Download failed.");
    }

    private int selectInitialMirror() {
        String domain = Locale.getDefault().getCountry();
        if (domain != null) {
            domain = "." + domain.toLowerCase() + "/";
            if (domain.equals(".us/")) {
                domain = ".com/";
            } else if (domain.equals(".gb/")) {
                domain = ".uk/";
            }
            for (int i = 0; i < MIRRORS.length; i++) {
                if (MIRRORS[i].indexOf(domain) != -1) {
                    return i;
                }
            }
        }
        return ((int) (Math.random() * 1000));
    }

    public Downloader(String url, String file) throws MalformedURLException {
        this.current = file;
        this.url = new URL(url + file);
        System.out.println("Trying mirror: " + url);
    }

    public static void main(String[] args) throws MalformedURLException, InterruptedException {
        new Downloader();
    }

    public void run() {
        try {
            InputStream is = url.openStream();
            File tempFile = File.createTempFile("q2-temp", null);
            tempFile.deleteOnExit();
            System.out.print("Downloading");
            copyStream(is, new FileOutputStream(tempFile), patches.get(current));
            System.out.println();
            System.out.println("Download finished; uncompressing");
            if (current.equals(Q320)) {
                new WinZipExtractor(tempFile);
            } else {

                ZipFile zipFile = new ZipFile(tempFile);
                Enumeration<?> e = zipFile.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    String name = entry.getName();
                    int i = 0;
                    if (hiresPak || (i = name.indexOf("/baseq2")) > -1 && name.indexOf(".dll") == -1) {
                        // hiresPak is rooted beneath baseq2
                        if (hiresPak) {
                            name = "/baseq2/" + name;
                        }
                        File outFile = new File("raw", name.substring(i));
                        if (entry.isDirectory()) {
                            outFile.mkdirs();
                        } else {
                            System.out.println("installing " + name.substring(i));
                            outFile.getParentFile().mkdirs();
                            copyStream(zipFile.getInputStream(entry),
                                       new FileOutputStream(outFile), -1);
                        }
                    }
                }
            }
            ok = true;
        } catch (IOException e) {
            System.out.println("Donwload Failed " + e);
        }
    }

    void copyStream(InputStream is, OutputStream os, int expected_size) throws IOException {
        int total = 0;
        int dots = 0;

        while (true) {
            int count = is.read(buf, 0, buf.length);
            if (count <= 0) {
                break;
            }
            total += count;
            if (expected_size != -1) {
                if (total / 1000000 > dots) {
                    System.out.print(".");
                    dots++;
                }
            }
            os.write(buf, 0, count);
        }
        if (expected_size != -1 && total != expected_size) {
            throw new IOException("Download errot: expected file size: " + expected_size + "; received: " + total);
        }

        os.close();
        is.close();
    }

    public void setHiresPak(boolean hiresPak) {
        this.hiresPak = hiresPak;
    }
}
