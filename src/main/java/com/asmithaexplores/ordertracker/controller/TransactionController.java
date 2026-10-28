package com.asmithaexplores.ordertracker.controller;

import com.asmithaexplores.ordertracker.dto.TransactionFilter;
import com.asmithaexplores.ordertracker.dto.TransactionForm;
import com.asmithaexplores.ordertracker.dto.TransactionView;
import com.asmithaexplores.ordertracker.entity.TransactionCategory;
import com.asmithaexplores.ordertracker.entity.TransactionType;
import com.asmithaexplores.ordertracker.service.TransactionService;
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

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionTagService tagService;

    @ModelAttribute("activeTab")
    public String activeTab() {
        return "transactions";
    }

    @GetMapping
    public String list(@ModelAttribute("filter") TransactionFilter filter, Model model) {
        List<TransactionView> transactions = transactionService.search(filter);
        model.addAttribute("transactions", transactions);
        model.addAttribute("summary", transactionService.summarise(transactions));
        addPickLists(model);
        return "transactions/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("transactionForm", TransactionForm.builder()
                .category(TransactionCategory.PRINTING)
                .transactionDate(LocalDate.now())
                .build());
        addFormAttributes(model, false, null);
        return "transactions/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TransactionForm form = transactionService.getForm(id);
        model.addAttribute("transactionForm", form);
        addFormAttributes(model, true, form.getOrderId());
        return "transactions/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("transactionForm") TransactionForm transactionForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model, transactionForm.getId() != null, transactionForm.getOrderId());
            return "transactions/form";
        }
        transactionService.save(transactionForm);
        redirectAttributes.addFlashAttribute("successMessage", "Transaction saved successfully.");
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        transactionService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Transaction deleted.");
        return "redirect:/transactions";
    }

    private void addFormAttributes(Model model, boolean editing, Long orderId) {
        model.addAttribute("editing", editing);
        model.addAttribute("linkedOrderId", orderId);
        addPickLists(model);
    }

    private void addPickLists(Model model) {
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("incomeCategories", TransactionCategory.of(TransactionType.INCOME));
        model.addAttribute("expenseCategories", TransactionCategory.of(TransactionType.EXPENSE));
        model.addAttribute("tags", tagService.findAll());
    }
}
