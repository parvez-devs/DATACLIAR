package com.axiom.datacliar;
interface IControlService {
    String execute(String action, String packageName, int userId);
    void destroy() = 16777114;
}
