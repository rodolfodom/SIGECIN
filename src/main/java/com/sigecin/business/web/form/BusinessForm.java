package com.sigecin.business.web.form;

import com.sigecin.business.dto.BusinessData;
import com.sigecin.business.entity.Business;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Formulario de alta y edición del negocio. Los campos opcionales vacíos llegan como null. */
@Getter
@Setter
public class BusinessForm {

    // Mismo patrón que el atributo pattern del <input> (validación en el navegador)
    public static final String PHONE_PATTERN = "[0-9+() -]{7,20}";

    @NotNull
    private Integer categoryId;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 20)
    @Pattern(regexp = PHONE_PATTERN)
    private String phone;

    @Size(max = 255)
    private String address;

    public static BusinessForm from(Business business) {
        BusinessForm form = new BusinessForm();
        form.categoryId = business.getCategory().getId();
        form.name = business.getName();
        form.description = business.getDescription();
        form.phone = business.getPhone();
        form.address = business.getAddress();
        return form;
    }

    public BusinessData toData() {
        return new BusinessData(categoryId, name, description, phone, address);
    }
}
