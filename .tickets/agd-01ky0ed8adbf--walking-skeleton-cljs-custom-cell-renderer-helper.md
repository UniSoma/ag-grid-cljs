---
id: agd-01ky0ed8adbf
title: 'Walking skeleton: CLJS custom cell renderer helper'
status: open
type: task
priority: 1
mode: hitl
created: '2026-07-20T19:00:26.061235644Z'
updated: '2026-07-20T19:00:26.061235644Z'
parent: agd-01ky0ebxg01e
tags:
- wayfinder:prototype
deps:
- agd-01ky0ed83xww
---

## Description

## Question

Prove the cell-renderer ergonomics risk: a helper that lets users define custom cell renderers in ClojureScript against AG Grid's vanilla renderer interface (init/getGui/refresh). Explore at least: plain DOM/hiccup->DOM helper, and mounting a React/Fulcro component into the cell (portal or local root) for React-hosted apps. Decide which helper(s) the library commits to.
