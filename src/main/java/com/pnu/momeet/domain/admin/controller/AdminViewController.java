package com.pnu.momeet.domain.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminViewController {

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    @GetMapping
    public String adminMain() {
        return "admin/dashboard";
    }

    @GetMapping("/members")
    public String memberManagement() {
        return "admin/members";
    }

    @GetMapping("/profiles")
    public String profileManagement() {
        return "admin/profiles";
    }

    @GetMapping("/ws-test")
    public String wsTest() {
        return "admin/ws-test";
    }
}
