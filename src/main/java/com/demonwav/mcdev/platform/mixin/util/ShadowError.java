package com.demonwav.mcdev.platform.mixin.util;

import com.google.common.collect.Lists;

import java.util.List;

public final class ShadowError {

    /**
     * Default is false, which means warning. True is error.
     */
    private boolean isError = false;

    private Object[] errorContextInfos;

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    public Object[] getErrorContextInfos() {
        return errorContextInfos;
    }

    public void setErrorContextInfos(Object[] errorContextInfos) {
        this.errorContextInfos = errorContextInfos;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean isError = false;
        private List<Object> errorContextInfos = Lists.newArrayList();

        public ShadowError build() {
            ShadowError error = new ShadowError();
            error.setError(isError);
            error.setErrorContextInfos(errorContextInfos.toArray());
            return error;
        }

        public Builder setError(boolean isError) {
            this.isError = isError;
            return this;
        }

        public Builder addContext(Object context) {
            errorContextInfos.add(context);
            return this;
        }
    }
}
