package com.asmithaexplores.ordertracker.controller;

import com.asmithaexplores.ordertracker.dto.ProductForm;
import com.asmithaexplores.ordertracker.dto.RestockResult;
import com.asmithaexplores.ordertracker.service.ProductService;
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

import java.math.BigDecimal;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @ModelAttribute("activeTab")
    public String activeTab() {
        return "products";
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("categories", productService.findCategories());
        return "products/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("productForm", ProductForm.builder()
                .category("Flash Cards")
                .price(BigDecimal.ZERO)
                .quantityInHand(0)
                .quantityToBeOrdered(0)
                .build());
        model.addAttribute("categories", productService.findCategories());
        model.addAttribute("editing", false);
        return "products/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("productForm", productService.getForm(id));
        model.addAttribute("categories", productService.findCategories());
        model.addAttribute("editing", true);
        return "products/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("productForm") ProductForm productForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", productService.findCategories());
            model.addAttribute("editing", productForm.getId() != null);
            return "products/form";
        }
        productService.save(productForm);
        redirectAttributes.addFlashAttribute("successMessage", "Product saved successfully.");
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Product deleted.");
        return "redirect:/products";
    }

    /**
     * Dashboard "Ordered" button: the units needed by the open orders have been
     * printed/ordered, so they go into stock and the pending amount is cleared.
     */
    @PostMapping("/{id}/ordered")
    public String markOrdered(@PathVariable Long id,
                              @RequestParam(value = "redirect", required = false) String redirect,
                              RedirectAttributes redirectAttributes) {
        RestockResult result = productService.markOrdered(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Added " + result.quantityAdded() + " × " + result.productName()
                        + " to stock (now " + result.newQuantityInHand() + " in hand).");
        return "redirect:" + (redirect == null || redirect.isBlank() ? "/" : redirect);
    }
}
