/*
 * This file is part of ardapaths, licensed under the MIT License (MIT).
 *
 * Copyright (c) Paul-Bantz <https://github.com/Paul-Bantz>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package space.ajcool.ardapaths.api;

/**
 * Entrypoint interface for mods that want to integrate with the ardapaths API.
 *
 * <p>Implement this interface and register your class in your mod's {@code fabric.mod.json}
 * under the {@code ardapaths:api} entrypoint key. Your implementation will be instantiated
 * by Fabric and {@link #onApiReady(ArdaPathsApi)} will be called during ArdaPaths
 * initialization.</p>
 *
 * <h2>Registration example ({@code fabric.mod.json})</h2>
 * <pre>{@code
 * "entrypoints": {
 *   "ardapaths:api": [
 *     "com.example.mymod.ArdaPathsHook"
 *   ]
 * }
 * }</pre>
 *
 * <h2>Implementation example</h2>
 * <pre>{@code
 * public class ArdaPathsHook implements ArdaPathsApiEntrypoint {
 *     @Override
 *     public void onApiReady(ArdaPathsApi ardaPathsApi) {
 *         ...;
 *     }
 * }
 * }</pre>
 */
public interface ArdaPathsApiEntrypoint {

    /**
     * Called when ArdaPaths initializes its client-side API instance.
     *
     * <p>API methods that mutate client state, including
     * {@link ArdaPathsApi#selectPathAndChapter(String, String, boolean, boolean)}, must be
     * called from the client thread.</p>
     *
     * @param ardaPathsApi the API instance, providing access to ardapaths features and registration methods
     */
    void onApiReady(ArdaPathsApi ardaPathsApi);
}
