package com.axiom.datacliar;
interface IControlService {
    String execute(String action, String packageName, int userId) = 0;
    void destroy() = 16777114;
}
