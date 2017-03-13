package com.miapc.ipudong.vo;

import java.util.List;

/**
 * Created by wangwei on 2016/11/15.
 */
public class ResouceVO {
    private List<Resouce> Result;

    public List<Resouce> getResult() {
        return Result;
    }

    public void setResult(List<Resouce> result) {
        Result = result;
    }
}

class Resouce {
    private String ResourceTypeId;
    private String ResourceTypeName;
    private int Sort;

    public String getResourceTypeId() {
        return ResourceTypeId;
    }

    public void setResourceTypeId(String resourceTypeId) {
        ResourceTypeId = resourceTypeId;
    }

    public String getResourceTypeName() {
        return ResourceTypeName;
    }

    public void setResourceTypeName(String resourceTypeName) {
        ResourceTypeName = resourceTypeName;
    }

    public int getSort() {
        return Sort;
    }

    public void setSort(int sort) {
        Sort = sort;
    }
}