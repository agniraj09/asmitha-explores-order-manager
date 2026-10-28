package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.dto.TransactionTagForm;
import com.asmithaexplores.ordertracker.dto.TransactionTagView;
import com.asmithaexplores.ordertracker.entity.TransactionTag;
import com.asmithaexplores.ordertracker.exception.BusinessRuleException;
import com.asmithaexplores.ordertracker.exception.ResourceNotFoundException;
import com.asmithaexplores.ordertracker.repository.TransactionRepository;
import com.asmithaexplores.ordertracker.repository.TransactionTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Maintains the tag list used to segregate money by what it was for: "Flash Cards", "Banners",
 * "Courier" and anything else the owner adds.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionTagService {

    /** Created once on an empty tag table so the Finance screens are usable from the first run. */
    private static final List<String[]> DEFAULT_TAGS = List.of(
            new String[]{"Flash Cards", "#9F1239"},
            new String[]{"Banners", "#B45309"},
            new String[]{"Learning Books", "#1D4ED8"},
            new String[]{"Story Books", "#C2410C"},
            new String[]{"Courier", "#047857"},
            new String[]{"Packing material", "#6D28D9"},
            new String[]{"General", "#475569"});

    private final TransactionTagRepository tagRepository;
    private final TransactionRepository transactionRepository;

    public List<TransactionTagView> findAll() {
        return tagRepository.findAllByOrderByNameAsc().stream().map(this::toView).toList();
    }

    public TransactionTag getEntity(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tag", id));
    }

    public TransactionTagForm getForm(Long id) {
        TransactionTag tag = getEntity(id);
        return TransactionTagForm.builder()
                .id(tag.getId())
                .name(tag.getName())
                .colorHex(tag.getColorHex())
                .description(tag.getDescription())
                .build();
    }

    /** Resolves the tag ids picked on a transaction form, silently skipping unknown ids. */
    public Set<TransactionTag> resolve(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(tagRepository.findAllById(ids));
    }

    /** Tags whose name matches one of the given names, ignoring case. Used to auto-tag orders. */
    public Set<TransactionTag> findByNames(Collection<String> names) {
        Set<TransactionTag> found = new LinkedHashSet<>();
        if (names == null) {
            return found;
        }
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                tagRepository.findByNameIgnoreCase(name.trim()).ifPresent(found::add);
            }
        }
        return found;
    }

    @Transactional
    public TransactionTag save(TransactionTagForm form) {
        String name = form.getName().trim();
        tagRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(form.getId())) {
                throw new BusinessRuleException("A tag named \"" + name + "\" already exists.");
            }
        });

        TransactionTag tag = form.getId() == null ? new TransactionTag() : getEntity(form.getId());
        tag.setName(name);
        tag.setColorHex(trimToNull(form.getColorHex()));
        tag.setDescription(trimToNull(form.getDescription()));
        return tagRepository.save(tag);
    }

    @Transactional
    public void delete(Long id) {
        TransactionTag tag = getEntity(id);
        if (transactionRepository.existsByTagsId(id)) {
            throw new BusinessRuleException(
                    "Cannot delete \"" + tag.getName() + "\" because transactions are still tagged with it.");
        }
        tagRepository.delete(tag);
    }

    /** Fills an empty tag table with a sensible starter set. Does nothing once tags exist. */
    @Transactional
    public void createDefaultsIfEmpty() {
        if (tagRepository.count() > 0) {
            return;
        }
        DEFAULT_TAGS.forEach(entry -> tagRepository.save(TransactionTag.builder()
                .name(entry[0])
                .colorHex(entry[1])
                .build()));
    }

    public TransactionTagView toView(TransactionTag tag) {
        return TransactionTagView.builder()
                .id(tag.getId())
                .name(tag.getName())
                .colorHex(tag.getColorHex())
                .description(tag.getDescription())
                .inUse(transactionRepository.existsByTagsId(tag.getId()))
                .build();
    }

    /** Light view without the "in use" lookup - for tag pills rendered inside a transaction row. */
    public static TransactionTagView toPill(TransactionTag tag) {
        return TransactionTagView.builder()
                .id(tag.getId())
                .name(tag.getName())
                .colorHex(tag.getColorHex())
                .description(tag.getDescription())
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
