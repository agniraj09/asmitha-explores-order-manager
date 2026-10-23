package com.asmithaexplores.ordertracker.controller;

import com.asmithaexplores.ordertracker.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/** Printable shipping labels for the open orders. */
@Controller
@RequestMapping("/labels")
@RequiredArgsConstructor
public class AddressLabelController {

    private final OrderService orderService;

    @ModelAttribute("activeTab")
    public String activeTab() {
        return "labels";
    }

    @GetMapping("/addresses")
    public String addresses(Model model) {
        model.addAttribute("labels", orderService.findLabelCandidates());
        return "labels/addresses";
    }
}
