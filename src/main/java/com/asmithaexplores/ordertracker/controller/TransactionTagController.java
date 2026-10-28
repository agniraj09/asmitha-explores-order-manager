package com.asmithaexplores.ordertracker.controller;

import com.asmithaexplores.ordertracker.dto.TransactionTagForm;
import com.asmithaexplores.ordertracker.service.TransactionTagService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/transactions/tags")
@RequiredArgsConstructor
public class TransactionTagController {

    private final TransactionTagService tagService;

    @ModelAttribute("activeTab")
    public String activeTab() {
        return "transactions";
    }

    @GetMapping
    public String list(Model model) {
        if (!model.containsAttribute("tagForm")) {
            model.addAttribute("tagForm", TransactionTagForm.builder().colorHex("#475569").build());
        }
        model.addAttribute("tags", tagService.findAll());
        return "transactions/tags";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("tagForm", tagService.getForm(id));
        model.addAttribute("tags", tagService.findAll());
        return "transactions/tags";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("tagForm") TransactionTagForm tagForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tags", tagService.findAll());
            return "transactions/tags";
        }
        tagService.save(tagForm);
        redirectAttributes.addFlashAttribute("successMessage", "Tag saved successfully.");
        return "redirect:/transactions/tags";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        tagService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Tag deleted.");
        return "redirect:/transactions/tags";
    }
}
