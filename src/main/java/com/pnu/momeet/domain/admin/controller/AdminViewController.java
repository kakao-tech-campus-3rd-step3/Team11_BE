package com.pnu.momeet.domain.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    public String adminMain(Model model) {
        model.addAttribute("title", "관리자 대시보드");
        model.addAttribute("content", "admin/dashboard :: content");
        return "admin/layout";
    }

    @GetMapping("/members")
    public String memberManagement(Model model) {
        model.addAttribute("title", "회원 관리");
        model.addAttribute("content", "admin/members :: content");
        return "admin/layout";
    }

    @GetMapping("/profiles")
    public String profileManagement(Model model) {
        model.addAttribute("title", "프로필 관리");
        model.addAttribute("content", "admin/profiles :: content");
        return "admin/layout";
    }
}
