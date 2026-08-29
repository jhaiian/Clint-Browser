(function() {
    if (window.__clintSelectPickerInit) return;
    window.__clintSelectPickerInit = true;

    var nextId = 1;
    var registry = {};

    function labelForSelect(select) {
        var aria = select.getAttribute('aria-label');
        if (aria) return aria;
        if (select.id) {
            var labelEl = document.querySelector('label[for="' + CSS.escape(select.id) + '"]');
            if (labelEl && labelEl.textContent) return labelEl.textContent.trim();
        }
        var parentLabel = select.closest('label');
        if (parentLabel) {
            var clone = parentLabel.cloneNode(true);
            var innerSelect = clone.querySelector('select');
            if (innerSelect) innerSelect.remove();
            var text = clone.textContent.trim();
            if (text) return text;
        }
        return '';
    }

    function collectOptions(select) {
        var options = [];
        var children = select.children;
        for (var i = 0; i < children.length; i++) {
            var node = children[i];
            if (node.tagName === 'OPTION') {
                options.push({
                    value: node.value,
                    label: node.textContent,
                    selected: node.selected,
                    disabled: node.disabled,
                    group: null
                });
            } else if (node.tagName === 'OPTGROUP') {
                var groupOptions = node.querySelectorAll('option');
                for (var j = 0; j < groupOptions.length; j++) {
                    var opt = groupOptions[j];
                    options.push({
                        value: opt.value,
                        label: opt.textContent,
                        selected: opt.selected,
                        disabled: opt.disabled || node.disabled,
                        group: node.label
                    });
                }
            }
        }
        return options;
    }

    function openPicker(select) {
        var id = select.getAttribute('data-clint-select-id');
        if (!id) {
            id = String(nextId++);
            select.setAttribute('data-clint-select-id', id);
        }
        registry[id] = select;
        var options = collectOptions(select);
        SelectPickerBridge.onSelectOpen(id, JSON.stringify(options), !!select.multiple, labelForSelect(select));
    }

    function intercept(e) {
        var target = e.target;
        if (target && target.tagName === 'SELECT' && !target.disabled) {
            e.preventDefault();
            e.stopPropagation();
            target.blur();
            openPicker(target);
        }
    }

    document.addEventListener('mousedown', intercept, true);
    document.addEventListener('click', intercept, true);

    window.__clintApplySelect = function(id, valuesJson) {
        var select = registry[id];
        if (!select) return;
        var values = JSON.parse(valuesJson);
        if (select.multiple) {
            var opts = select.options;
            for (var i = 0; i < opts.length; i++) {
                opts[i].selected = values.indexOf(opts[i].value) !== -1;
            }
        } else if (values.length > 0) {
            var setter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, 'value').set;
            if (setter) {
                setter.call(select, values[0]);
            } else {
                select.value = values[0];
            }
        }
        select.dispatchEvent(new Event('input', { bubbles: true }));
        select.dispatchEvent(new Event('change', { bubbles: true }));
    };
})();
