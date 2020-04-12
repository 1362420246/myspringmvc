package com.qbk.servlet.v3.webmvc.servlet;

import lombok.Data;

import java.util.Map;

/**
 * ModelAndView
 */
@Data
public class QBKModelAndView {
    private String viewName;
    private Map<String,?> model;

    public QBKModelAndView(String viewName, Map<String, ?> model) {
        this.viewName = viewName;
        this.model = model;
    }

    public QBKModelAndView(String viewName) {
        this.viewName = viewName;
    }
}


