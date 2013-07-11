package com.amd.aparapi.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *  We can use this Annotation to 'tag' intended constant buffers.
 *
 *  So we can either annotate the buffer
 *  <pre><code>
 *  &#64Constant int[] buffer = new int[1024];
 *  </code></pre>
 *   Or use a special suffix
 *  <pre><code>
 *  int[] buffer_$constant$ = new int[1024];
 *  </code></pre>
 *
 *  @see LOCAL_SUFFIX
 *
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Constant {

    /**
    *  We can use this suffix to 'tag' intended constant buffers.
    *
    *
    *  So either name the buffer
    *  <pre><code>
    *  int[] buffer_$constant$ = new int[1024];
    *  </code></pre>
    *  Or use the Annotation form
    *  <pre><code>
    *  &#64Constant int[] buffer = new int[1024];
    *  </code></pre>
    */
    String CONSTANT_SUFFIX = "_$constant$";
}
