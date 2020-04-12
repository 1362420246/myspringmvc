package com.qbk.servlet.v3.webmvc.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * View 视图
 */
public class QBKView {

    /**
     * 模板
     */
    private File viewFile;

    public QBKView(File templateFile) {
        this.viewFile = templateFile;
    }

    /**
     * 页面渲染
     * @param model model
     * @param resp resp
     */
    public void render(Map<String, ?> model, HttpServletResponse resp) throws Exception {
        StringBuilder sb = new StringBuilder();

        //文件流
        RandomAccessFile ra = new RandomAccessFile(this.viewFile,"r");

        String line = null;
        while (null != (line = ra.readLine())){
            line = new String(line.getBytes("ISO-8859-1"),"utf-8");
            // 编译正则表达式
            Pattern pattern = Pattern.compile("￥\\{[^\\}]+\\}",Pattern.CASE_INSENSITIVE);
            //匹配
            Matcher matcher = pattern.matcher(line);
            //尝试从头开始查询字符串时候有和正则表达式匹配的部分
            while (matcher.find()){
                String paramName = matcher.group();
                //取到模板中的变量
                paramName = paramName.replaceAll("￥\\{|\\}","");
                //从model中拿到值
                Object paramValue = model.get(paramName);
                //处理特殊字符
                String value = makeStringForRegExp(paramValue.toString());
                //只替换find() 找到的第一个
                line = matcher.replaceFirst(value);
                matcher = pattern.matcher(line);
            }
            sb.append(line);
        }
        resp.setCharacterEncoding("utf-8");
        resp.getWriter().write(sb.toString());
    }

    /**
     * 处理特殊字符
     */
    private static String makeStringForRegExp(String str) {
        return str.replace("\\", "\\\\").replace("*", "\\*")
                .replace("+", "\\+").replace("|", "\\|")
                .replace("{", "\\{").replace("}", "\\}")
                .replace("(", "\\(").replace(")", "\\)")
                .replace("^", "\\^").replace("$", "\\$")
                .replace("[", "\\[").replace("]", "\\]")
                .replace("?", "\\?").replace(",", "\\,")
                .replace(".", "\\.").replace("&", "\\&");
    }
}
