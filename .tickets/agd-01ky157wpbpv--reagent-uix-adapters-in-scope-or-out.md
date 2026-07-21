---
id: agd-01ky157wpbpv
title: 'Reagent/UIx adapters: in scope or out?'
status: open
type: task
priority: 3
mode: hitl
created: '2026-07-21T01:39:27.563335742Z'
updated: '2026-07-21T01:39:27.563335742Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:grilling
---

## Description

## Question

react/react-renderer already accepts any React element, so Reagent (r/as-element) and UIx ($) components mount today via one line of user composition. Decide: does the library ship thin adapter sugar (e.g. reagent-renderer / uix-renderer namespaces with their own optional deps), document the composition recipe only, or rule adapters out of scope? Context: cell-renderer resolution on agd-01ky0ed8adbf (per-cell local root committed; portal question parked on the Fulcro skeleton).
