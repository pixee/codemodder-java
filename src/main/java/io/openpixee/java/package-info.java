/**
 * This package holds the Java operations for file discovery and rewriting.
 *
 * <p>Note for Mac developers -- the behavior of {@link java.io.File#getAbsolutePath()}, {@link
 * java.io.File#getCanonicalPath()} appear to have some hard-to-predict differences, so we try to
 * use {@link java.io.File#getCanonicalPath()} everywhere.
 */
package io.openpixee.java;
