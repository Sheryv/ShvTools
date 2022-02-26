package com.sheryv.tools.cloudservermanager.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/panel")
class PanelController {
  
  @GetMapping("")
  fun panelPage(): String {
    return "panel"
  }
}
