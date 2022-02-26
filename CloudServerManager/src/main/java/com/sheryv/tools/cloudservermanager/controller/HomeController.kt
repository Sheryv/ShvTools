package com.sheryv.tools.cloudservermanager.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.view.RedirectView

@Controller
class HomeController {
  
  @GetMapping("/")
  fun mainPage(): RedirectView {
    return RedirectView("/login")
  }
  
  
  @GetMapping("/login")
  fun loginPage(): String {
    return "login"
  }
}
