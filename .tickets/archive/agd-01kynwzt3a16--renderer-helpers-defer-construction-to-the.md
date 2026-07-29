---
id: agd-01kynwzt3a16
title: Renderer helpers defer construction to the boundary
status: closed
type: bug
priority: 2
mode: afk
created: '2026-07-29T02:59:17.222279778Z'
updated: '2026-07-29T12:21:48.584486832Z'
closed: '2026-07-29T12:21:48.584486832Z'
tags:
- render
- react
- convert
acceptance:
- title: Each of the three helpers returns = values across two calls given the same (stable) input
  done: true
- title: The three helpers join the table-driven builder/helper check
  done: true
- title: A rebuilt options map whose :column-defs carry a dom-renderer over a stable render fn produces zero setGridOption calls through update-grid!
  done: true
- title: 'Renderer behavior is unchanged: existing render and browser renderer tests pass untouched, and react-dom stays off the classpath for consumers who do not require the react namespace'
  done: true
- title: The renderer caveat is removed from updating-data.md and ADR 0021 open-gap note
  done: true
deps:
- agd-01kynwzbcmnt
---

## Description

render/renderer, render/dom-renderer and react/react-renderer each build a fresh component class per call and return it raw-wrapped. Called during render — the normal shape for a React-family consumer — the value nests inside :column-defs, which is an ordinary updatable key, so the differ sees it as changed on every render and re-applies the whole columnDefs value. That is redundant churn plus the column-state reset ADR 0008 documents as "may, not guaranteed". It degrades to churn rather than breakage, which is why it was split from the two fixes that came before it.

This is the open gap ADR 0021 names. Closing it makes the rebuild-stability promise true across the whole public surface.

## Design

Each helper tags the user's input, not the constructed value. This is the load-bearing detail: dom-renderer and react-renderer each build a fresh lifecycle map of three fresh closures over the render fn before delegating to renderer, so tagging at the renderer level leaves the value non-= even with a perfectly stable render fn and the fix fixes nothing.

Three tags, each registered by the namespace that owns its construction: :renderer carries the lifecycle map with its method in render; :dom-renderer carries the render fn, method in render; :react-renderer carries the render fn, method in react. react stays optional — its method only needs to exist if the consumer required the namespace that mints the value.

AG Grid detects a component class via `candidate.prototype && "getGui" in candidate.prototype`, so the constructed class still crosses the boundary raw. Building at conversion time rather than call time means a new class object reaches AG Grid only when the diff actually fired, which is exactly when re-creating cell components is acceptable.

Remaining consumer half, to state in the docs: an inline (fn [params] ...) passed to dom-renderer during render still churns. Defining render fns at namespace level is the idiomatic answer and is already how the repo writes them.

## Notes

**2026-07-29T12:21:48.584486832Z**

The three renderer helpers stash the consumer's input in a deferred value and the conversion boundary builds the component class (ADR 0021 §4), so two calls with the same input are = and a rebuilt options map no longer re-applies :column-defs. Tags are owned by the namespace that mints them — :renderer (lifecycle map) and :dom-renderer (render fn) in render, :react-renderer in react — so convert never requires react and react-dom stays off a core consumer's classpath. Tagging the input rather than the constructed value is load-bearing: both sugar helpers build a lifecycle map of fresh closures before delegating. The class still crosses the boundary raw; only timing moved, so AG Grid sees a new class exactly when the diff fired. Existing render and browser renderer tests untouched as the evidence; the helpers joined the rebuild-stability table and rebuilt-opts now carries a dom-renderer. Docs: caveat removed from updating-data.md, ADR 0021 staging notes closed out, ADR 0011 correction note added. Node 91 tests / 299 assertions, browser 14 / 45, both green.
