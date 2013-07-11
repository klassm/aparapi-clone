package com.amd.aparapi;

import com.amd.aparapi.annotation.OpenCLMapping;
import com.amd.aparapi.internal.util.UnsafeWrapper;

public class GPUMethods {

    /**
     * Delegates to either {@link java.lang.Math#acos(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/acos.html">acos(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param a value to delegate to {@link java.lang.Math#acos(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/acos.html">acos(float)</a></code>
     * @return {@link java.lang.Math#acos(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/acos.html">acos(float)</a></code>
     *
     * @see java.lang.Math#acos(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/acos.html">acos(float)</a></code>
     */
    @OpenCLMapping(mapTo = "acos")
    public static float acos(float a) {
        return (float) Math.acos(a);
    }

    /**
     * Delegates to either {@link java.lang.Math#acos(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/acos.html">acos(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param a value to delegate to {@link java.lang.Math#acos(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/acos.html">acos(double)</a></code>
     * @return {@link java.lang.Math#acos(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/acos.html">acos(double)</a></code>
     *
     * @see java.lang.Math#acos(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/acos.html">acos(double)</a></code>
     */
    @OpenCLMapping(mapTo = "acos")
    public static double acos(double a) {
        return Math.acos(a);
    }

    /**
     * Delegates to either {@link java.lang.Math#asin(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/asin.html">asin(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#asin(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/asin.html">asin(float)</a></code>
     * @return {@link java.lang.Math#asin(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/asin.html">asin(float)</a></code>
     *
     * @see java.lang.Math#asin(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/asin.html">asin(float)</a></code>
     */
    @OpenCLMapping(mapTo = "asin")
    public static float asin(float _f) {
        return (float) Math.asin(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#asin(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/asin.html">asin(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#asin(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/asin.html">asin(double)</a></code>
     * @return {@link java.lang.Math#asin(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/asin.html">asin(double)</a></code>
     *
     * @see java.lang.Math#asin(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/asin.html">asin(double)</a></code>
     */
    @OpenCLMapping(mapTo = "asin")
    public static double asin(double _d) {
        return Math.asin(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#atan(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#atan(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan(float)</a></code>
     * @return {@link java.lang.Math#atan(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan(float)</a></code>
     *
     * @see java.lang.Math#atan(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan(float)</a></code>
     */
    @OpenCLMapping(mapTo = "atan")
    public static float atan(float _f) {
        return (float) Math.atan(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#atan(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#atan(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan(double)</a></code>
     * @return {@link java.lang.Math#atan(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan(double)</a></code>
     *
     * @see java.lang.Math#atan(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan(double)</a></code>
     */
    @OpenCLMapping(mapTo = "atan")
    public static double atan(double _d) {
        return Math.atan(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#atan2(double, double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan2(float, float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f1 value to delegate to first argument of {@link java.lang.Math#atan2(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan2(float, float)</a></code>
     * @param _f2 value to delegate to second argument of {@link java.lang.Math#atan2(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan2(float, float)</a></code>
     * @return {@link java.lang.Math#atan2(double, double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan2(float, float)</a></code>
     *
     * @see java.lang.Math#atan2(double, double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan2(float, float)</a></code>
     */
    @OpenCLMapping(mapTo = "atan2")
    public static float atan2(float _f1, float _f2) {
        return (float) Math.atan2(_f1, _f2);
    }

    /**
     * Delegates to either {@link java.lang.Math#atan2(double, double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan2(double, double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d1 value to delegate to first argument of {@link java.lang.Math#atan2(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan2(double, double)</a></code>
     * @param _d2 value to delegate to second argument of {@link java.lang.Math#atan2(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan2(double, double)</a></code>
     * @return {@link java.lang.Math#atan2(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan2(double, double)</a></code>
     *
     * @see java.lang.Math#atan2(double, double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atan.html">atan2(double, double)</a></code>
     */
    @OpenCLMapping(mapTo = "atan2")
    public static double atan2(double _d1, double _d2) {
        return Math.atan2(_d1, _d2);
    }

    /**
     * Delegates to either {@link java.lang.Math#ceil(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/ceil.html">ceil(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#ceil(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/ceil.html">ceil(float)</a></code>
     * @return {@link java.lang.Math#ceil(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/ceil.html">ceil(float)</a></code>
     *
     * @see java.lang.Math#ceil(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/ceil.html">ceil(float)</a></code>
     */
    @OpenCLMapping(mapTo = "ceil")
    public static float ceil(float _f) {
        return (float) Math.ceil(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#ceil(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/ceil.html">ceil(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#ceil(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/ceil.html">ceil(double)</a></code>
     * @return {@link java.lang.Math#ceil(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/ceil.html">ceil(double)</a></code>
     *
     * @see java.lang.Math#ceil(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/ceil.html">ceil(double)</a></code>
     */
    @OpenCLMapping(mapTo = "ceil")
    public static double ceil(double _d) {
        return Math.ceil(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#cos(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/cos.html">cos(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#cos(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/cos.html">cos(float)</a></code>
     * @return {@link java.lang.Math#cos(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/cos.html">cos(float)</a></code>
     *
     * @see java.lang.Math#cos(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/cos.html">cos(float)</a></code>
     */
    @OpenCLMapping(mapTo = "cos")
    public static float cos(float _f) {
        return (float) Math.cos(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#cos(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/cos.html">cos(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#cos(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/cos.html">cos(double)</a></code>
     * @return {@link java.lang.Math#cos(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/cos.html">cos(double)</a></code>
     *
     * @see java.lang.Math#cos(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/cos.html">cos(double)</a></code>
     */
    @OpenCLMapping(mapTo = "cos")
    public static double cos(double _d) {
        return Math.cos(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#exp(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/exp.html">exp(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#exp(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/exp.html">exp(float)</a></code>
     * @return {@link java.lang.Math#exp(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/exp.html">exp(float)</a></code>
     *
     * @see java.lang.Math#exp(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/exp.html">exp(float)</a></code>
     */
    @OpenCLMapping(mapTo = "exp")
    public static float exp(float _f) {
        return (float) Math.exp(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#exp(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/exp.html">exp(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#exp(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/exp.html">exp(double)</a></code>
     * @return {@link java.lang.Math#exp(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/exp.html">exp(double)</a></code>
     *
     * @see java.lang.Math#exp(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/exp.html">exp(double)</a></code>
     */
    @OpenCLMapping(mapTo = "exp")
    public static double exp(double _d) {
        return Math.exp(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#abs(float)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fabs.html">fabs(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#abs(float)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fabs.html">fabs(float)</a></code>
     * @return {@link java.lang.Math#abs(float)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fabs.html">fabs(float)</a></code>
     *
     * @see java.lang.Math#abs(float)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fabs.html">fabs(float)</a></code>
     */
    @OpenCLMapping(mapTo = "fabs")
    public static float abs(float _f) {
        return Math.abs(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#abs(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fabs.html">fabs(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#abs(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fabs.html">fabs(double)</a></code>
     * @return {@link java.lang.Math#abs(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fabs.html">fabs(double)</a></code>
     *
     * @see java.lang.Math#abs(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fabs.html">fabs(double)</a></code>
     */
    @OpenCLMapping(mapTo = "fabs")
    public static double abs(double _d) {
        return Math.abs(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#abs(int)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/abs.html">abs(int)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param n value to delegate to {@link java.lang.Math#abs(int)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/abs.html">abs(int)</a></code>
     * @return {@link java.lang.Math#abs(int)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/abs.html">abs(int)</a></code>
     *
     * @see java.lang.Math#abs(int)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/abs.html">abs(int)</a></code>
     */
    @OpenCLMapping(mapTo = "abs")
    public static int abs(int n) {
        return Math.abs(n);
    }

    /**
     * Delegates to either {@link java.lang.Math#abs(long)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/abs.html">abs(long)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param n value to delegate to {@link java.lang.Math#abs(long)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/abs.html">abs(long)</a></code>
     * @return {@link java.lang.Math#abs(long)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fabs.html">abs(long)</a></code>
     *
     * @see java.lang.Math#abs(long)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/abs.html">abs(long)</a></code>
     */
    @OpenCLMapping(mapTo = "abs")
    public static long abs(long n) {
        return Math.abs(n);
    }

    /**
     * Delegates to either {@link java.lang.Math#floor(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/abs.html">floor(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#floor(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/floor.html">floor(float)</a></code>
     * @return {@link java.lang.Math#floor(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/floor.html">floor(float)</a></code>
     *
     * @see java.lang.Math#floor(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/floor.html">floor(float)</a></code>
     */
    @OpenCLMapping(mapTo = "floor")
    public static float floor(float _f) {
        return (float) Math.floor(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#floor(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/abs.html">floor(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#floor(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/floor.html">floor(double)</a></code>
     * @return {@link java.lang.Math#floor(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/floor.html">floor(double)</a></code>
     *
     * @see java.lang.Math#floor(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/floor.html">floor(double)</a></code>
     */
    @OpenCLMapping(mapTo = "floor")
    public static double floor(double _d) {
        return Math.floor(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#max(float, float)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmax.html">fmax(float, float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f1 value to delegate to first argument of {@link java.lang.Math#max(float, float)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmax.html">fmax(float, float)</a></code>
     * @param _f2 value to delegate to second argument of {@link java.lang.Math#max(float, float)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmax.html">fmax(float, float)</a></code>
     * @return {@link java.lang.Math#max(float, float)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmax.html">fmax(float, float)</a></code>
     *
     * @see java.lang.Math#max(float, float)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmax.html">fmax(float, float)</a></code>
     */
    @OpenCLMapping(mapTo = "fmax")
    public static float max(float _f1, float _f2) {
        return Math.max(_f1, _f2);
    }

    /**
     * Delegates to either {@link java.lang.Math#max(double, double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmax.html">fmax(double, double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d1 value to delegate to first argument of {@link java.lang.Math#max(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmax.html">fmax(double, double)</a></code>
     * @param _d2 value to delegate to second argument of {@link java.lang.Math#max(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmax.html">fmax(double, double)</a></code>
     * @return {@link java.lang.Math#max(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmax.html">fmax(double, double)</a></code>
     *
     * @see java.lang.Math#max(double, double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmax.html">fmax(double, double)</a></code>
     */
    @OpenCLMapping(mapTo = "fmax")
    public static double max(double _d1, double _d2) {
        return Math.max(_d1, _d2);
    }

    /**
     * Delegates to either {@link java.lang.Math#max(int, int)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">max(int, int)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param n1 value to delegate to {@link java.lang.Math#max(int, int)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">max(int, int)</a></code>
     * @param n2 value to delegate to {@link java.lang.Math#max(int, int)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">max(int, int)</a></code>
     * @return {@link java.lang.Math#max(int, int)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">max(int, int)</a></code>
     *
     * @see java.lang.Math#max(int, int)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">max(int, int)</a></code>
     */
    @OpenCLMapping(mapTo = "max")
    public static int max(int n1, int n2) {
        return Math.max(n1, n2);
    }

    /**
     * Delegates to either {@link java.lang.Math#max(long, long)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">max(long, long)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param n1 value to delegate to first argument of {@link java.lang.Math#max(long, long)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">max(long, long)</a></code>
     * @param n2 value to delegate to second argument of {@link java.lang.Math#max(long, long)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">max(long, long)</a></code>
     * @return {@link java.lang.Math#max(long, long)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">max(long, long)</a></code>
     *
     * @see java.lang.Math#max(long, long)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">max(long, long)</a></code>
     */
    @OpenCLMapping(mapTo = "max")
    public static long max(long n1, long n2) {
        return Math.max(n1, n2);
    }

    /**
     * Delegates to either {@link java.lang.Math#min(float, float)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmin.html">fmin(float, float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f1 value to delegate to first argument of {@link java.lang.Math#min(float, float)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmin.html">fmin(float, float)</a></code>
     * @param _f2 value to delegate to second argument of {@link java.lang.Math#min(float, float)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmin.html">fmin(float, float)</a></code>
     * @return {@link java.lang.Math#min(float, float)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmin.html">fmin(float, float)</a></code>
     *
     * @see java.lang.Math#min(float, float)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmin.html">fmin(float, float)</a></code>
     */
    @OpenCLMapping(mapTo = "fmin")
    public static float min(float _f1, float _f2) {
        return Math.min(_f1, _f2);
    }

    /**
     * Delegates to either {@link java.lang.Math#min(double, double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmin.html">fmin(double, double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d1 value to delegate to first argument of {@link java.lang.Math#min(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmin.html">fmin(double, double)</a></code>
     * @param _d2 value to delegate to second argument of {@link java.lang.Math#min(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmin.html">fmin(double, double)</a></code>
     * @return {@link java.lang.Math#min(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmin.html">fmin(double, double)</a></code>
     *
     * @see java.lang.Math#min(double, double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/fmin.html">fmin(double, double)</a></code>
     */
    @OpenCLMapping(mapTo = "fmin")
    public static double min(double _d1, double _d2) {
        return Math.min(_d1, _d2);
    }

    /**
     * Delegates to either {@link java.lang.Math#min(int, int)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">min(int, int)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param n1 value to delegate to first argument of {@link java.lang.Math#min(int, int)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">min(int, int)</a></code>
     * @param n2 value to delegate to second argument of {@link java.lang.Math#min(int, int)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">min(int, int)</a></code>
     * @return {@link java.lang.Math#min(int, int)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">min(int, int)</a></code>
     *
     * @see java.lang.Math#min(int, int)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">min(int, int)</a></code>
     */
    @OpenCLMapping(mapTo = "min")
    public static int min(int n1, int n2) {
        return Math.min(n1, n2);
    }

    /**
     * Delegates to either {@link java.lang.Math#min(long, long)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">min(long, long)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param n1 value to delegate to first argument of {@link java.lang.Math#min(long, long)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">min(long, long)</a></code>
     * @param n2 value to delegate to second argument of {@link java.lang.Math#min(long, long)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">min(long, long)</a></code>
     * @return {@link java.lang.Math#min(long, long)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">min(long, long)</a></code>
     *
     * @see java.lang.Math#min(long, long)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/integerMax.html">min(long, long)</a></code>
     */
    @OpenCLMapping(mapTo = "min")
    public static long min(long n1, long n2) {
        return Math.min(n1, n2);
    }

    /**
     * Delegates to either {@link java.lang.Math#log(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/log.html">log(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#log(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/log.html">log(float)</a></code>
     * @return {@link java.lang.Math#log(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/log.html">log(float)</a></code>
     *
     * @see java.lang.Math#log(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/log.html">log(float)</a></code>
     */
    @OpenCLMapping(mapTo = "log")
    public static float log(float _f) {
        return (float) Math.log(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#log(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/log.html">log(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#log(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/log.html">log(double)</a></code>
     * @return {@link java.lang.Math#log(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/log.html">log(double)</a></code>
     *
     * @see java.lang.Math#log(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/log.html">log(double)</a></code>
     */
    @OpenCLMapping(mapTo = "log")
    public static double log(double _d) {
        return Math.log(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#pow(double, double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/pow.html">pow(float, float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f1 value to delegate to first argument of {@link java.lang.Math#pow(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/pow.html">pow(float, float)</a></code>
     * @param _f2 value to delegate to second argument of {@link java.lang.Math#pow(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/pow.html">pow(float, float)</a></code>
     * @return {@link java.lang.Math#pow(double, double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/pow.html">pow(float, float)</a></code>
     *
     * @see java.lang.Math#pow(double, double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/pow.html">pow(float, float)</a></code>
     */
    @OpenCLMapping(mapTo = "pow")
    public static float pow(float _f1, float _f2) {
        return (float) Math.pow(_f1, _f2);
    }

    /**
     * Delegates to either {@link java.lang.Math#pow(double, double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/pow.html">pow(double, double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d1 value to delegate to first argument of {@link java.lang.Math#pow(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/pow.html">pow(double, double)</a></code>
     * @param _d2 value to delegate to second argument of {@link java.lang.Math#pow(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/pow.html">pow(double, double)</a></code>
     * @return {@link java.lang.Math#pow(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/pow.html">pow(double, double)</a></code>
     *
     * @see java.lang.Math#pow(double, double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/pow.html">pow(double, double)</a></code>
     */
    @OpenCLMapping(mapTo = "pow")
    public static double pow(double _d1, double _d2) {
        return Math.pow(_d1, _d2);
    }

    /**
     * Delegates to either {@link java.lang.Math#IEEEremainder(double, double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/remainder.html">remainder(float, float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f1 value to delegate to first argument of {@link java.lang.Math#IEEEremainder(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/remainder.html">remainder(float, float)</a></code>
     * @param _f2 value to delegate to second argument of {@link java.lang.Math#IEEEremainder(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/remainder.html">remainder(float, float)</a></code>
     * @return {@link java.lang.Math#IEEEremainder(double, double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/remainder.html">remainder(float, float)</a></code>
     *
     * @see java.lang.Math#IEEEremainder(double, double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/remainder.html">remainder(float, float)</a></code>
     */
    @OpenCLMapping(mapTo = "remainder")
    public static float IEEEremainder(float _f1, float _f2) {
        return (float) Math.IEEEremainder(_f1, _f2);
    }

    /**
     * Delegates to either {@link java.lang.Math#IEEEremainder(double, double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/remainder.html">remainder(double, double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d1 value to delegate to first argument of {@link java.lang.Math#IEEEremainder(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/remainder.html">remainder(double, double)</a></code>
     * @param _d2 value to delegate to second argument of {@link java.lang.Math#IEEEremainder(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/remainder.html">remainder(double, double)</a></code>
     * @return {@link java.lang.Math#IEEEremainder(double, double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/remainder.html">remainder(double, double)</a></code>
     *
     * @see java.lang.Math#IEEEremainder(double, double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/remainder.html">remainder(double, double)</a></code>
     */
    @OpenCLMapping(mapTo = "remainder")
    public static double IEEEremainder(double _d1, double _d2) {
        return Math.IEEEremainder(_d1, _d2);
    }

    /**
     * Delegates to either {@link java.lang.Math#toRadians(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/radians.html">radians(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#toRadians(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/radians.html">radians(float)</a></code>
     * @return {@link java.lang.Math#toRadians(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/radians.html">radians(float)</a></code>
     *
     * @see java.lang.Math#toRadians(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/radians.html">radians(float)</a></code>
     */
    @OpenCLMapping(mapTo = "radians")
    public static float toRadians(float _f) {
        return (float) Math.toRadians(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#toRadians(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/radians.html">radians(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#toRadians(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/radians.html">radians(double)</a></code>
     * @return {@link java.lang.Math#toRadians(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/radians.html">radians(double)</a></code>
     *
     * @see java.lang.Math#toRadians(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/radians.html">radians(double)</a></code>
     */
    @OpenCLMapping(mapTo = "radians")
    public static double toRadians(double _d) {
        return Math.toRadians(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#toDegrees(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/degrees.html">degrees(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#toDegrees(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/degrees.html">degrees(float)</a></code>
     * @return {@link java.lang.Math#toDegrees(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/degrees.html">degrees(float)</a></code>
     *
     * @see java.lang.Math#toDegrees(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/degrees.html">degrees(float)</a></code>
     */
    @OpenCLMapping(mapTo = "degrees")
    public static float toDegrees(float _f) {
        return (float) Math.toDegrees(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#toDegrees(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/degrees.html">degrees(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#toDegrees(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/degrees.html">degrees(double)</a></code>
     * @return {@link java.lang.Math#toDegrees(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/degrees.html">degrees(double)</a></code>
     *
     * @see java.lang.Math#toDegrees(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/degrees.html">degrees(double)</a></code>
     */
    @OpenCLMapping(mapTo = "degrees")
    public static double toDegrees(double _d) {
        return Math.toDegrees(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#rint(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/rint.html">rint(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#rint(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/rint.html">rint(float)</a></code>
     * @return {@link java.lang.Math#rint(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/rint.html">rint(float)</a></code>
     *
     * @see java.lang.Math#rint(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/rint.html">rint(float)</a></code>
     */
    @OpenCLMapping(mapTo = "rint")
    public static float rint(float _f) {
        return (float) Math.rint(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#rint(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/rint.html">rint(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#rint(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/rint.html">rint(double)</a></code>
     * @return {@link java.lang.Math#rint(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/rint.html">rint(double)</a></code>
     *
     * @see java.lang.Math#rint(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/rint.html">rint(double)</a></code>
     */
    @OpenCLMapping(mapTo = "rint")
    public static double rint(double _d) {
        return Math.rint(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#round(float)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/round.html">round(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#round(float)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/round.html">round(float)</a></code>
     * @return {@link java.lang.Math#round(float)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/round.html">round(float)</a></code>
     *
     * @see java.lang.Math#round(float)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/round.html">round(float)</a></code>
     */
    @OpenCLMapping(mapTo = "round")
    public static int round(float _f) {
        return Math.round(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#round(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/round.html">round(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#round(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/round.html">round(double)</a></code>
     * @return {@link java.lang.Math#round(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/round.html">round(double)</a></code>
     *
     * @see java.lang.Math#round(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/round.html">round(double)</a></code>
     */
    @OpenCLMapping(mapTo = "round")
    public static long round(double _d) {
        return Math.round(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#sin(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sin.html">sin(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#sin(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sin.html">sin(float)</a></code>
     * @return {@link java.lang.Math#sin(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sin.html">sin(float)</a></code>
     *
     * @see java.lang.Math#sin(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sin.html">sin(float)</a></code>
     */
    @OpenCLMapping(mapTo = "sin")
    public static float sin(float _f) {
        return (float) Math.sin(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#sin(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sin.html">sin(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#sin(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sin.html">sin(double)</a></code>
     * @return {@link java.lang.Math#sin(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sin.html">sin(double)</a></code>
     *
     * @see java.lang.Math#sin(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sin.html">sin(double)</a></code>
     */
    @OpenCLMapping(mapTo = "sin")
    public static double sin(double _d) {
        return Math.sin(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#sqrt(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">sqrt(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#sqrt(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">sqrt(float)</a></code>
     * @return {@link java.lang.Math#sqrt(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">sqrt(float)</a></code>
     *
     * @see java.lang.Math#sqrt(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">sqrt(float)</a></code>
     */
    @OpenCLMapping(mapTo = "sqrt")
    public static float sqrt(float _f) {
        return (float) Math.sqrt(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#sqrt(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">sqrt(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#sqrt(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">sqrt(double)</a></code>
     * @return {@link java.lang.Math#sqrt(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">sqrt(double)</a></code>
     *
     * @see java.lang.Math#sqrt(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">sqrt(double)</a></code>
     */
    @OpenCLMapping(mapTo = "sqrt")
    public static double sqrt(double _d) {
        return Math.sqrt(_d);
    }

    /**
     * Delegates to either {@link java.lang.Math#tan(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/tan.html">tan(float)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#tan(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/tan.html">tan(float)</a></code>
     * @return {@link java.lang.Math#tan(double)} casted to float/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/tan.html">tan(float)</a></code>
     *
     * @see java.lang.Math#tan(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/tan.html">tan(float)</a></code>
     */
    @OpenCLMapping(mapTo = "tan")
    public static float tan(float _f) {
        return (float) Math.tan(_f);
    }

    /**
     * Delegates to either {@link java.lang.Math#tan(double)} (Java) or <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/tan.html">tan(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#tan(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/tan.html">tan(double)</a></code>
     * @return {@link java.lang.Math#tan(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/tan.html">tan(double)</a></code>
     *
     * @see java.lang.Math#tan(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/tan.html">tan(double)</a></code>
     */
    @OpenCLMapping(mapTo = "tan")
    public static double tan(double _d) {
        return Math.tan(_d);
    }

    // the following rsqrt and native_sqrt and native_rsqrt don't exist in java Math
    // but added them here for nbody testing, not sure if we want to expose them
    /**
     * Computes  inverse square root using {@link java.lang.Math#sqrt(double)} (Java) or delegates to <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">rsqrt(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _f value to delegate to {@link java.lang.Math#sqrt(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">rsqrt(double)</a></code>
     * @return <code>( 1.0f / {@link java.lang.Math#sqrt(double)} casted to float )</code>/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">rsqrt(double)</a></code>
     *
     * @see java.lang.Math#sqrt(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">rsqrt(double)</a></code>
     */
    @OpenCLMapping(mapTo = "rsqrt")
    public static float rsqrt(float _f) {
        return (1.0f / (float) Math.sqrt(_f));
    }

    /**
     * Computes  inverse square root using {@link java.lang.Math#sqrt(double)} (Java) or delegates to <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">rsqrt(double)</a></code> (OpenCL).
     *
     * User should note the differences in precision between Java and OpenCL's implementation of arithmetic functions to determine whether the difference in precision is acceptable.
     *
     * @param _d value to delegate to {@link java.lang.Math#sqrt(double)}/<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">rsqrt(double)</a></code>
     * @return <code>( 1.0f / {@link java.lang.Math#sqrt(double)} )</code> /<code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">rsqrt(double)</a></code>
     *
     * @see java.lang.Math#sqrt(double)
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/sqrt.html">rsqrt(double)</a></code>
     */
    @OpenCLMapping(mapTo = "rsqrt")
    public static double rsqrt(double _d) {
        return (1.0 / Math.sqrt(_d));
    }

    @OpenCLMapping(mapTo = "native_sqrt")
    public static float native_sqrt(float _f) {
        int j = Float.floatToIntBits(_f);
        j = ((1 << 29) + (j >> 1)) - (1 << 22) - 0x4c00;
        return (Float.intBitsToFloat(j));
        // could add more precision using one iteration of newton's method, use the following
    }

    @OpenCLMapping(mapTo = "native_rsqrt")
    public static float native_rsqrt(float _f) {
        int j = Float.floatToIntBits(_f);
        j = 0x5f3759df - (j >> 1);
        final float x = (Float.intBitsToFloat(j));
        return x;
        // if want more precision via one iteration of newton's method, use the following
        // float fhalf = 0.5f*_f;
        // return (x *(1.5f - fhalf * x * x));
    }

    // Hacked from AtomicIntegerArray.getAndAdd(i, delta)
    /**
     * Atomically adds <code>_delta</code> value to <code>_index</code> element of array <code>_arr</code> (Java) or delegates to <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atomic_add.html">atomic_add(volatile int*, int)</a></code> (OpenCL).
     *
     *
     * @param _arr array for which an element value needs to be atomically incremented by <code>_delta</code>
     * @param _index index of the <code>_arr</code> array that needs to be atomically incremented by <code>_delta</code>
     * @param _delta value by which <code>_index</code> element of <code>_arr</code> array needs to be atomically incremented  
     * @return previous value of <code>_index</code> element of <code>_arr</code> array
     *
     * @see <code><a href="http://www.khronos.org/registry/cl/sdk/1.1/docs/man/xhtml/atomic_add.html">atomic_add(volatile int*, int)</a></code>
     */
    @OpenCLMapping(atomic32 = true)
    public static int atomicAdd(int[] _arr, int _index, int _delta) {
        if (!Config.disableUnsafe) {
            return UnsafeWrapper.atomicAdd(_arr, _index, _delta);
        } else {
            synchronized (_arr) {
                final int previous = _arr[_index];
                _arr[_index] += _delta;
                return previous;
            }
        }
    }
}
