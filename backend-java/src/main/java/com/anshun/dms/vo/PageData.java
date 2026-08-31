package com.anshun.dms.vo;

import java.util.List;

/** Keeps the existing frontend-friendly total/data structure inside ApiResponse. */
public record PageData<T>(long total, List<T> data) { }
