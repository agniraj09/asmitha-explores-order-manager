package com.asmithaexplores.ordertracker.controller;

import com.asmithaexplores.ordertracker.dto.CustomerForm;
import com.asmithaexplores.ordertracker.exception.BusinessRuleException;
import com.asmithaexplores.ordertracker.service.CustomerService;
import com.asmithaexplores.ordertracker.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;

    @ModelAttribute("activeTab")
    public String activeTab() {
        return "customers";
    }

    @GetMapping
    public String list(@RequestParam(value = "search", required = false) String search, Model model) {
        model.addAttribute("customers", customerService.findAll(search));
        model.addAttribute("search", search == null ? "" : search);
        return "customers/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("customerForm", new CustomerForm());
        model.addAttribute("editing", false);
        return "customers/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("customerForm", customerService.getForm(id));
        model.addAttribute("editing", true);
        return "customers/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("customerForm") CustomerForm customerForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", customerForm.getId() != null);
            return "customers/form";
        }
        try {
            customerService.save(customerForm);
        } catch (BusinessRuleException ex) {
            bindingResult.rejectValue("mobileNumber", "duplicate", ex.getMessage());
            model.addAttribute("editing", customerForm.getId() != null);
            return "customers/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Customer saved successfully.");
        return "redirect:/customers";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.getView(id));
        model.addAttribute("orders", orderService.findByCustomer(id));
        return "customers/detail";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        customerService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Customer deleted.");
        return "redirect:/customers";
    }
}

