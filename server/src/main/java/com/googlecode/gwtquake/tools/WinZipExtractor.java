package com.googlecode.gwtquake.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * https://stackoverflow.com/questions/7924895/how-can-i-read-from-a-winzip-self-extracting-exe-zip-file-in-java
 * @author Dmitrii Tikhomirov
 * @author James Allman
 * Created by treblereel 6/12/20
 */

public class WinZipExtractor {

    public WinZipExtractor(File tempFile) throws IOException {
        File dir = new File("raw/baseq2");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (ZipInputStream zis = new ZipInputStream(new WinZipInputStream(new FileInputStream(tempFile)))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().equals("baseq2/pak1.pak")) {
                    File file = new File(dir, ze.getName().replace("baseq2/", ""));
                    if (!file.exists()) {
                        FileOutputStream os = new FileOutputStream(file);
                        System.out.println("Extracting " + ze.getName());
                        for (int c = zis.read(); c != -1; c = zis.read()) {
                            os.write(c);
                        }
                        os.close();
                    }
                }
                zis.closeEntry();
            }
        }
    }

    public static class WinZipInputStream extends FilterInputStream {

        public static final byte[] ZIP_LOCAL = {0x50, 0x4b, 0x03, 0x04};
        protected int ip;
        protected int op;

        public WinZipInputStream(InputStream is) {
            super(is);
        }

        public int read() throws IOException {
            while (ip < ZIP_LOCAL.length) {
                int c = super.read();
                if (c == ZIP_LOCAL[ip]) {
                    ip++;
                } else {
                    ip = 0;
                }
            }

            if (op < ZIP_LOCAL.length) {
                return ZIP_LOCAL[op++];
            } else {
                return super.read();
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (op == ZIP_LOCAL.length) {
                return super.read(b, off, len);
            }
            int l = 0;
            while (l < Math.min(len, ZIP_LOCAL.length)) {
                b[l++] = (byte) read();
            }
            return l;
        }
    }
}
