/*
 * Validación en el navegador para los formularios con la clase "needs-validation".
 * Replica las reglas del servidor para avisar antes de enviar. El servidor sigue
 * validando todo: esto solo ahorra el viaje cuando el error es evidente.
 *
 * Reglas nativas: required, type="email", minlength/maxlength, min/max, pattern, step.
 * Reglas propias (atributos data-*, con el id del otro campo):
 *   data-match="id"          debe ser igual a otro campo (confirmar contraseña)
 *   data-required-when="id"  obligatorio solo si la casilla indicada está marcada
 *   data-after="id"          debe ser mayor que otro campo (hora de cierre > apertura)
 * Textos propios por campo: data-size-message, data-pattern-message,
 * data-step-message y data-after-message.
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

    const DEPENDENCIES = ['match', 'requiredWhen', 'after'];

    function applyCustomRules(field) {
        let message = '';
        const value = field.value;
        const data = field.dataset;
        if (data.requiredWhen) {
            const toggle = document.getElementById(data.requiredWhen);
            if (toggle && toggle.checked && !value) {
                message = MESSAGES.required;
            }
        }
        if (!message && value && field.minLength > 0 && value.length < field.minLength) {
            // Se revisa a mano: tooShort solo se activa con ciertas formas de edición
            message = formatSize(field);
        }
        if (!message && data.match) {
            const other = document.getElementById(data.match);
            if (other && value && value !== other.value) {
                message = MESSAGES.mismatch;
            }
        }
        if (!message && data.after) {
            // Sirve para horas "HH:mm" y fechas ISO: se comparan como texto
            const other = document.getElementById(data.after);
            if (other && value && other.value && value <= other.value) {
                message = data.afterMessage || MESSAGES.invalid;
            }
        }
        field.setCustomValidity(message);
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
        if (validity.rangeUnderflow) return (MESSAGES.min || '').replace('{1}', field.min);
        if (validity.rangeOverflow) return (MESSAGES.max || '').replace('{1}', field.max);
        if (validity.patternMismatch) return field.dataset.patternMessage || MESSAGES.invalid;
        if (validity.stepMismatch) return field.dataset.stepMessage || MESSAGES.invalid;
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
        // (antes del primer envío solo se corrigen los campos que ya estaban marcados)
        const shouldValidate = field => submitted || field.classList.contains('is-invalid');
        const onChange = event => {
            const changed = event.target;
            const fields = fieldsOf(form);
            if (fields.includes(changed) && shouldValidate(changed)) {
                validateField(changed);
            }
            // Revalida los campos que dependen del que cambió (confirmación, horas, casillas)
            fields.filter(other => DEPENDENCIES.some(key => other.dataset[key] === changed.id))
                .filter(shouldValidate)
                .forEach(validateField);
        };
        form.addEventListener('input', onChange);
        // Las casillas no siempre disparan "input"
        form.addEventListener('change', onChange);
    }

    document.querySelectorAll('form.needs-validation').forEach(setUp);
})();
