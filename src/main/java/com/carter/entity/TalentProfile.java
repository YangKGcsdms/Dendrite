package com.carter.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Talent profile entity containing AI-generated bilingual summaries.
 *
 * @author Carter
 * @since 1.0.0
 */
@Entity
@Getter
@Setter
@Table(name = "dendrite_profiles")
public class TalentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String employeeName;

    /**
     * Chinese professional summary.
     */
    @Column(name = "summary_zh", length = 2000)
    private String summaryZh;

    /**
     * English professional summary.
     */
    @Column(name = "summary_en", length = 2000)
    private String summaryEn;

    /**
     * Chinese skill tags.
     */
    @ElementCollection
    @CollectionTable(name = "profile_skills_zh")
    private List<String> skillsZh;

    /**
     * English skill tags.
     */
    @ElementCollection
    @CollectionTable(name = "profile_skills_en")
    private List<String> skillsEn;

    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Convert(converter = com.carter.converter.VectorToStringConverter.class)
    @Column(columnDefinition = "vector(768)")
    @org.hibernate.annotations.ColumnTransformer(write = "?::vector", read = "embedding::text")
    @JsonIgnore
    private List<Double> embedding;

    // ==========================================
    // Legacy compatibility (for existing code)
    // ==========================================
    
    @Transient
    @JsonIgnore
    public String getProfessionalSummary() {
        return summaryZh;
    }

    @Transient
    public void setProfessionalSummary(String summary) {
        this.summaryZh = summary;
    }

    @Transient
    @JsonIgnore
    public List<String> getTopSkills() {
        return skillsZh;
    }

    @Transient
    public void setTopSkills(List<String> skills) {
        this.skillsZh = skills;
    }
}
