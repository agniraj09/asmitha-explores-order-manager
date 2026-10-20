package com.asmithaexplores.ordertracker.controller;

import com.asmithaexplores.ordertracker.config.AppProperties;
import com.asmithaexplores.ordertracker.dto.CustomerForm;
import com.asmithaexplores.ordertracker.dto.OrderCreationResult;
import com.asmithaexplores.ordertracker.dto.OrderEditForm;
import com.asmithaexplores.ordertracker.dto.OrderFilter;
import com.asmithaexplores.ordertracker.dto.OrderForm;
import com.asmithaexplores.ordertracker.dto.OrderItemForm;
import com.asmithaexplores.ordertracker.dto.OrderView;
import com.asmithaexplores.ordertracker.entity.Customer;
import com.asmithaexplores.ordertracker.entity.OrderStatus;
import com.asmithaexplores.ordertracker.exception.BusinessRuleException;
import com.asmithaexplores.ordertracker.service.CustomerService;
import com.asmithaexplores.ordertracker.service.OrderService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    private final OrderService orderService;
    private final CustomerService customerService;
    private final ProductService productService;
    private final AppProperties appProperties;

    @ModelAttribute("activeTab")
    public String activeTab() {
        return "orders";
    }

    /** Pre-fills the courier tracking link on the detail page; the owner can override it there. */
    @ModelAttribute("trackingUrl")
    public String trackingUrl() {
        return appProperties.getTrackingUrl();
    }

    /** Token the browser replaces with the tracking link typed by the owner. */
    @ModelAttribute("trackingUrlPlaceholder")
    public String trackingUrlPlaceholder() {
        return OrderView.TRACKING_URL_PLACEHOLDER;
    }

    @GetMapping
    public String list(@ModelAttribute("filter") OrderFilter filter, Model model) {
        List<OrderView> orders = orderService.search(filter);
        model.addAttribute("orders", orders);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("customers", customerService.findAllCustomers());
        return "orders/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(value = "customerId", required = false) Long customerId, Model model) {
        OrderForm form = new OrderForm();
        // "Add a new customer" is the default; pre-selecting a customer switches to the picker.
        if (customerId != null) {
            form.setNewCustomer(false);
            form.setCustomerId(customerId);
        }
        form.getItems().add(new OrderItemForm());
        model.addAttribute("orderForm", form);
        addFormReferenceData(model);
        return "orders/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("orderForm") OrderForm orderForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        validateCustomerSelection(orderForm, bindingResult);

        if (bindingResult.hasErrors()) {
            ensureAtLeastOneRow(orderForm);
            addFormReferenceData(model);
            return "orders/form";
        }

        if (orderForm.isNewCustomer()) {
            try {
                Customer customer = customerService.save(CustomerForm.builder()
                        .name(orderForm.getNewCustomerName())
                        .address(orderForm.getNewCustomerAddress())
                        .mobileNumber(orderForm.getNewCustomerMobileNumber())
                        .build());
                orderForm.setCustomerId(customer.getId());
            } catch (BusinessRuleException ex) {
                bindingResult.rejectValue("newCustomerMobileNumber", "duplicate", ex.getMessage());
                ensureAtLeastOneRow(orderForm);
                addFormReferenceData(model);
                return "orders/form";
            }
        }

        OrderCreationResult result = orderService.create(orderForm);
        redirectAttributes.addFlashAttribute("successMessage", "Order #" + result.getOrderId() + " created.");
        if (result.hasWarnings()) {
            redirectAttributes.addFlashAttribute("warningMessages", result.getWarnings());
        }
        return "redirect:/orders/" + result.getOrderId();
    }

    /** Full edit (customer, date, products, notes) - only while the order is still NEW. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        OrderForm form = orderService.getEditForm(id);
        ensureAtLeastOneRow(form);
        model.addAttribute("orderForm", form);
        addFormReferenceData(model);
        return "orders/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("orderForm") OrderForm orderForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        orderForm.setId(id);
        validateCustomerSelection(orderForm, bindingResult);

        if (bindingResult.hasErrors()) {
            ensureAtLeastOneRow(orderForm);
            addFormReferenceData(model);
            return "orders/form";
        }

        if (orderForm.isNewCustomer()) {
            try {
                Customer customer = customerService.save(CustomerForm.builder()
                        .name(orderForm.getNewCustomerName())
                        .address(orderForm.getNewCustomerAddress())
                        .mobileNumber(orderForm.getNewCustomerMobileNumber())
                        .build());
                orderForm.setCustomerId(customer.getId());
            } catch (BusinessRuleException ex) {
                bindingResult.rejectValue("newCustomerMobileNumber", "duplicate", ex.getMessage());
                ensureAtLeastOneRow(orderForm);
                addFormReferenceData(model);
                return "orders/form";
            }
        }

        OrderCreationResult result = orderService.update(orderForm);
        redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " updated.");
        if (result.hasWarnings()) {
            redirectAttributes.addFlashAttribute("warningMessages", result.getWarnings());
        }
        return "redirect:/orders/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        OrderView order = orderService.getView(id);
        model.addAttribute("order", order);
        if (!model.containsAttribute("orderEditForm")) {
            model.addAttribute("orderEditForm", OrderEditForm.builder()
                    .id(order.getId())
                    .customizationNotes(order.getCustomizationNotes())
                    .trackingId(order.getTrackingId())
                    .build());
        }
        model.addAttribute("statuses", OrderStatus.values());
        return "orders/detail";
    }

    @PostMapping("/{id}/details")
    public String updateDetails(@PathVariable Long id,
                                @Valid @ModelAttribute("orderEditForm") OrderEditForm orderEditForm,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        orderEditForm.setId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("order", orderService.getView(id));
            model.addAttribute("statuses", OrderStatus.values());
            return "orders/detail";
        }
        orderService.updateDetails(orderEditForm);
        redirectAttributes.addFlashAttribute("successMessage", "Order details updated.");
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("targetStatus") OrderStatus targetStatus,
                               @RequestParam(value = "trackingId", required = false) String trackingId,
                               RedirectAttributes redirectAttributes) {
        OrderView updated = orderService.updateStatus(id, targetStatus, trackingId);
        redirectAttributes.addFlashAttribute("successMessage",
                "Order #" + id + " is now " + updated.getOrderStatus().getLabel() + ".");
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        orderService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " deleted and stock restored.");
        return "redirect:/orders";
    }

    private void validateCustomerSelection(OrderForm orderForm, BindingResult bindingResult) {
        if (orderForm.isNewCustomer()) {
            if (isBlank(orderForm.getNewCustomerName())) {
                bindingResult.rejectValue("newCustomerName", "required", "Customer name is required");
            }
            if (isBlank(orderForm.getNewCustomerAddress())) {
                bindingResult.rejectValue("newCustomerAddress", "required", "Address is required");
            }
            String mobile = orderForm.getNewCustomerMobileNumber();
            if (isBlank(mobile)) {
                bindingResult.rejectValue("newCustomerMobileNumber", "required", "Mobile number is required");
            } else if (!MOBILE_PATTERN.matcher(mobile.trim()).matches()) {
                bindingResult.rejectValue("newCustomerMobileNumber", "pattern",
                        "Enter a valid 10-digit Indian mobile number (starting with 6, 7, 8 or 9)");
            }
        } else if (orderForm.getCustomerId() == null) {
            bindingResult.rejectValue("customerId", "required", "Select a customer or add a new one");
        }

        List<OrderItemForm> validItems = new ArrayList<>();
        if (orderForm.getItems() != null) {
            orderForm.getItems().stream()
                    .filter(i -> i != null && i.getProductId() != null)
                    .forEach(validItems::add);
        }
        if (validItems.isEmpty()) {
            bindingResult.rejectValue("items", "required", "Add at least one product to the order");
        }
    }

    private void addFormReferenceData(Model model) {
        model.addAttribute("customers", customerService.findAllCustomers());
        model.addAttribute("products", productService.findAll());
        model.addAttribute("productsByCategory", productService.findAllGroupedByCategory());
    }

    private void ensureAtLeastOneRow(OrderForm orderForm) {
        if (orderForm.getItems() == null) {
            orderForm.setItems(new ArrayList<>());
        }
        if (orderForm.getItems().isEmpty()) {
            orderForm.getItems().add(new OrderItemForm());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

