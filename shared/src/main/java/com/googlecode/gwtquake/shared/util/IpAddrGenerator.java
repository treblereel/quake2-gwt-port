package com.googlecode.gwtquake.shared.util;

import java.util.Random;

/**
 * @author Dmitrii Tikhomirov
 * Created by treblereel 5/25/20
 */
public class IpAddrGenerator {

    private static Random r = new Random();

    public static String get(int min, int max) {
        StringBuffer sb = new StringBuffer();
        sb.append(getRandomNumberInRange(min, max)).append(".");
        sb.append(getRandomNumberInRange(min, max)).append(".");
        sb.append(getRandomNumberInRange(min, max)).append(".");
        sb.append(getRandomNumberInRange(min, max));
        return sb.toString();
    }

    private static int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        return r.nextInt((max - min) + 1) + min;
    }
}
