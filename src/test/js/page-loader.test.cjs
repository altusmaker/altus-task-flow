const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {test} = require('node:test');
const {join} = require('node:path');
const vm = require('node:vm');

const source = readFileSync(join(__dirname, '../../main/resources/static/js/page-loader.js'), 'utf8');

function run(type, referrer = '', pathname = '/') {
    const attributes = {};
    const timers = [];
    let onLoad;
    let removed = false;
    const loader = {remove() { removed = true; }, classList: {add() {}}};
    vm.runInNewContext(source, {
        URL,
        performance: {getEntriesByType: () => type ? [{type}] : []},
        location: {origin: 'http://localhost:8080', pathname},
        document: {
            referrer,
            documentElement: {setAttribute: name => { attributes[name] = ''; }},
            querySelector: () => loader
        },
        window: {
            addEventListener: (event, callback) => { onLoad = callback; },
            setTimeout: (callback, delay) => timers.push(delay)
        }
    });
    const suppressedBeforeLoad = Object.hasOwn(attributes, 'data-suppress-page-loader');
    onLoad();
    return {suppressedBeforeLoad, removed, timers};
}

test('first opening keeps the loading logo', () => {
    assert.deepEqual(run('navigate'), {suppressedBeforeLoad: false, removed: false, timers: [1500, 1700]});
});

test('task mutation reloads hide the logo before paint and remove it immediately', () => {
    assert.deepEqual(run('reload'), {suppressedBeforeLoad: true, removed: true, timers: []});
});

test('same-page submissions and filter changes do not show the logo', () => {
    assert.equal(run('navigate', 'http://localhost:8080/?filter=old').suppressedBeforeLoad, true);
});

test('navigation between home and worklog keeps the logo', () => {
    assert.equal(run('navigate', 'http://localhost:8080/worklog-report').suppressedBeforeLoad, false);
    assert.equal(run('navigate', 'http://localhost:8080/', '/worklog-report').suppressedBeforeLoad, false);
});

test('history navigation, external arrivals, and missing timing data keep the logo', () => {
    assert.equal(run('back_forward', 'http://localhost:8080/').suppressedBeforeLoad, false);
    assert.equal(run('navigate', 'https://example.com/').suppressedBeforeLoad, false);
    assert.equal(run(undefined).suppressedBeforeLoad, false);
});
