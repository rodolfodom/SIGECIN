/*
 * Validación en el navegador para los formularios con la clase "needs-validation".
 * Replica las reglas del servidor (atributos HTML: required, type="email",
 * minlength, maxlength y data-match) para avisar antes de enviar. El servidor
 * sigue validando todo: esto solo ahorra el viaje cuando el error es evidente.
 *
 * Los errores se muestran igual que los del servidor: clase is-invalid en el campo
 * y el texto en su .invalid-feedback. Los textos vienen de messages.properties
 * (window.SIGECIN_MESSAGES, definido en layout/base.html).
 */
(function () {
    'use strict';

    const MESSAGES = window.SIGECIN_MESSAGES || {};

    function fieldsOf(form) {
        return Array.from(form.querySelectorAll('input, select, textarea'))
            .filter(field => field.willValidate && field.type !== 'radio' && field.type !== 'checkbox');
    }

    // Regla de data-match="idDelOtroCampo" (p. ej. confirmar contraseña)
    function applyCustomRules(field) {
        let message = '';
        const value = field.value;
        if (value && field.minLength > 0 && value.length < field.minLength) {
            // Se revisa a mano: tooShort solo se activa con ciertas formas de edición
            message = formatSize(field);
        } else if (field.dataset.match) {
            const other = document.getElementById(field.dataset.match);
            if (other && value && value !== other.value) {
                message = MESSAGES.mismatch;
            }
        }
        field.setCustomValidity(message || '');
    }

    // data-size-message permite un texto propio por campo (mismo que el del servidor)
    function formatSize(field) {
        return (field.dataset.sizeMessage || MESSAGES.size || '')
            .replace('{2}', field.minLength > 0 ? field.minLength : 0)
            .replace('{1}', field.maxLength > 0 ? field.maxLength : '');
    }

    function messageFor(field) {
        const validity = field.validity;
        if (validity.valueMissing) return MESSAGES.required;
        if (validity.typeMismatch && field.type === 'email') return MESSAGES.email;
        if (validity.tooShort || validity.tooLong) return formatSize(field);
        if (validity.customError) return field.validationMessage;
        return MESSAGES.invalid;
    }

    // th:errors solo genera el .invalid-feedback cuando el servidor reportó un error;
    // si no existe se crea al final del contenedor (Bootstrap lo muestra por ser hermano posterior)
    function feedbackOf(field) {
        let feedback = field.parentElement.querySelector('.invalid-feedback');
        if (!feedback) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            field.parentElement.appendChild(feedback);
        }
        return feedback;
    }

    function validateField(field) {
        applyCustomRules(field);
        const feedback = feedbackOf(field);
        if (field.checkValidity()) {
            field.classList.remove('is-invalid');
            return true;
        }
        field.classList.add('is-invalid');
        feedback.textContent = messageFor(field);
        return false;
    }

    function setUp(form) {
        let submitted = false;

        form.addEventListener('submit', event => {
            submitted = true;
            const invalid = fieldsOf(form).filter(field => !validateField(field));
            if (invalid.length > 0) {
                event.preventDefault();
                event.stopPropagation();
                invalid[0].focus();
            }
        });

        // Tras el primer intento de envío, los errores se actualizan mientras se escribe
        form.addEventListener('input', event => {
            const field = event.target;
            if (!submitted && !field.classList.contains('is-invalid')) {
                return;
            }
            validateField(field);
            // Si cambia la contraseña, se revisa de nuevo su confirmación
            fieldsOf(form)
                .filter(other => other.dataset.match === field.id && other.value)
                .forEach(validateField);
        });
    }

    document.querySelectorAll('form.needs-validation').forEach(setUp);
})();
