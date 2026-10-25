const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {test} = require('node:test');
const {join} = require('node:path');
const vm = require('node:vm');

const source = readFileSync(join(__dirname, '../../main/resources/static/js/task-board.js'), 'utf8');

function element() {
    return {
        dataset: {}, attributes: {}, children: [], listeners: {}, textContent: '',
        setAttribute(name, value) { this.attributes[name] = value; },
        addEventListener(name, callback) { this.listeners[name] = callback; },
        appendChild(child) { this.children.push(child); },
        querySelectorAll() { return this.children; },
        click() { if (!this.disabled) this.listeners.click(); }
    };
}

function board(labels = [['تست', 'رفع خطا', ''], ['تست', ' سفارشی '], []]) {
    const container = element(), clear = element();
    const columns = labels.map(group => {
        const cards = group.map(label => ({dataset: {label}, hidden: false}));
        const count = element();
        return {
            cards, count,
            querySelector: () => count,
            querySelectorAll: () => cards.filter(card => !card.hidden)
        };
    });
    const cards = columns.flatMap(column => column.cards);
    const context = vm.createContext({
        bootstrap: {Modal: function () {}},
        document: {
            getElementById: id => id === 'quickFilterLabels' ? container : clear,
            querySelectorAll: selector => selector === '.column' ? columns : cards,
            createElement: element,
            addEventListener() {}
        }
    });
    vm.runInContext(source, context);
    vm.runInContext('setupLabelFilters()', context);
    return {
        container, clear, columns, cards,
        button: label => container.children.find(button => button.dataset.label === label),
        visible: () => cards.filter(card => !card.hidden)
    };
}

test('starts with all tasks visible, unique actual labels, and clear disabled', () => {
    const state = board();
    assert.equal(state.visible().length, 5);
    assert.equal(state.container.children.length, 3);
    assert.equal(state.clear.disabled, true);
    assert.ok(state.button('سفارشی'));
    assert.ok(state.container.children.every(button => button.attributes['aria-pressed'] === 'false'));
});

test('selecting a label filters every column and updates visible counts', () => {
    const state = board();
    state.button('تست').click();
    assert.equal(state.visible().length, 2);
    assert.deepEqual(state.columns.map(column => column.count.textContent), ['۱', '۱', '۰']);
    assert.equal(state.button('تست').attributes['aria-pressed'], 'true');
    assert.equal(state.clear.disabled, false);
});

test('multiple labels use OR matching and can be toggled independently', () => {
    const state = board();
    state.button('تست').click();
    state.button('سفارشی').click();
    assert.equal(state.visible().length, 3);
    state.button('تست').click();
    assert.equal(state.visible().length, 1);
    assert.equal(state.button('تست').attributes['aria-pressed'], 'false');
    state.button('سفارشی').click();
    assert.equal(state.visible().length, 5);
    assert.equal(state.clear.disabled, true);
});

test('clear removes every selection and restores unlabeled tasks too', () => {
    const state = board();
    state.button('تست').click();
    state.button('رفع خطا').click();
    state.clear.click();
    assert.equal(state.visible().length, 5);
    assert.equal(state.clear.disabled, true);
    assert.ok(state.container.children.every(button => button.attributes['aria-pressed'] === 'false'));
    assert.deepEqual(state.columns.map(column => column.count.textContent), ['۳', '۲', '۰']);
});

test('empty boards and boards without labels remain usable', () => {
    for (const labels of [[[], [], []], [['', undefined], [], []]]) {
        const state = board(labels);
        assert.equal(state.container.children.length, 0);
        assert.equal(state.visible().length, state.cards.length);
        assert.equal(state.clear.disabled, true);
    }
});
