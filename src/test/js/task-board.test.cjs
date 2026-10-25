const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {test} = require('node:test');
const {join} = require('node:path');
const vm = require('node:vm');

const source = readFileSync(join(__dirname, '../../main/resources/static/js/task-board.js'), 'utf8');

test('tray task links open the matching modal and ignore missing or invalid tasks', () => {
    for (const [search, expected] of [['?task=7', 1], ['?task=8', 0], ['?task=bad', 0], ['', 0]]) {
        const card = {id: 'task-7'}, opened = [];
        const context = vm.createContext({
            URLSearchParams,
            bootstrap: {Modal: function () {}},
            window: {location: {search}, openEditTaskModal: value => opened.push(value)},
            document: {getElementById: id => id === 'task-7' ? card : null, addEventListener() {}}
        });
        vm.runInContext(source, context);
        context.openLinkedTask();
        assert.equal(opened.length, expected);
        if (expected) assert.equal(opened[0], card);
    }
});

function dropBoard(status, hasWorklogs, response = {ok: true}) {
    const requests = [], alerts = [];
    let reloads = 0;
    const task = {dataset: {hasWorklogs: String(hasWorklogs)}};
    const context = vm.createContext({
        bootstrap: {Modal: function () {}},
        document: {getElementById: () => task, addEventListener() {}},
        alert: message => alerts.push(message),
        location: {reload: () => reloads++},
        fetch: (...args) => {
            requests.push(args);
            return response instanceof Error ? Promise.reject(response) : Promise.resolve(response);
        }
    });
    vm.runInContext(source, context);
    context.drop({
        preventDefault() {},
        currentTarget: {dataset: {status}, classList: {remove() {}}},
        dataTransfer: {getData: () => 'task-7'}
    });
    return {requests, alerts, reloads: () => reloads};
}

test('dropping a task with worklogs into TODO is blocked without a request', () => {
    const state = dropBoard('TODO', true);
    assert.equal(state.requests.length, 0);
    assert.equal(state.alerts.length, 1);
    assert.equal(state.reloads(), 0);
});

test('allowed drops persist status and refresh only after success', async () => {
    for (const [status, hasWorklogs] of [['TODO', false], ['IN_PROGRESS', true], ['DONE', true]]) {
        const state = dropBoard(status, hasWorklogs);
        assert.equal(state.requests[0][0], `/api/tasks/7/status?status=${status}`);
        assert.equal(state.requests[0][1].method, 'PATCH');
        assert.equal(state.reloads(), 0);
        await new Promise(resolve => setImmediate(resolve));
        assert.equal(state.reloads(), 1);
        assert.equal(state.alerts.length, 0);
    }
});

test('rejected or failed drops keep the board unchanged and show an error', async () => {
    for (const response of [{ok: false, status: 400}, new Error('Network unavailable')]) {
        const state = dropBoard('TODO', false, response);
        await new Promise(resolve => setImmediate(resolve));
        assert.equal(state.reloads(), 0);
        assert.equal(state.alerts.length, 1);
    }
});

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
