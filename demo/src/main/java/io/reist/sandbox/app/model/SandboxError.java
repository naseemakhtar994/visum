/*
 * Copyright (C) 2017 Renat Sarymsakov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reist.sandbox.app.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by m039 on 11/26/15.
 */
public class SandboxError {

    @SerializedName("message")
    private String message;

    private transient Throwable throwable;

    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public SandboxError(Throwable t) {
        throwable = t;
    }

}
