package com.sigecin.serviceoffering.web.form;

import com.sigecin.serviceoffering.dto.ServiceData;
import com.sigecin.serviceoffering.entity.ServiceOffering;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Formulario de alta y edición de un servicio. */
@Getter
@Setter
public class ServiceForm {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 255)
    private String description;

    // Una cita debe caber en un día de trabajo
    @NotNull
    @Min(5)
    @Max(480)
    private Integer durationMin;

    // DECIMAL(10,2) en la BD
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("99999999.99")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    public static ServiceForm from(ServiceOffering service) {
        ServiceForm form = new ServiceForm();
        form.name = service.getName();
        form.description = service.getDescription();
        form.durationMin = service.getDurationMin();
        form.price = service.getPrice();
        return form;
    }

    public ServiceData toData() {
        return new ServiceData(name, description, durationMin, price);
    }
}
