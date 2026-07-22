# AG Grid options reference

<!--
  GENERATED — do not edit by hand.
  AG Grid version: 36.0.2
  Regenerate with `cd tools/codegen && npm run generate` on an AG Grid
  dependency bump (tools/codegen/extract.mjs, ADR 0007). Never hand-edit.
-->

Kebab-case option and event names for AG Grid **36.0.2**, with the
camelCase name each converts to. Keys are ordinary EDN — the full surface is
reachable by `assoc`. For semantics of each option see the AG Grid docs:
<https://www.ag-grid.com/javascript-data-grid/grid-options/>.

## Grid Options

| kebab | camelCase | type | init-only? | deprecated → replacement | description |
| --- | --- | --- | --- | --- | --- |
| `accented-sort` | `accentedSort` | boolean |  |  | Set to `true` to specify that the sort should take accented characters into account. If this feature is turned on the sort will be slower. |
| `active-overlay` | `activeOverlay` | any |  |  | Display an overlay on demand. If provided takes precedence over the grid provided overlays. |
| `active-overlay-params` | `activeOverlayParams` | any |  |  | Custom parameters to be supplied to the `activeOverlay` component in addition to `IOverlayParams`. Updating the params will trigger a refresh of the active overlay. |
| `advanced-filter-builder-params` | `advancedFilterBuilderParams` | object |  |  | Customise the parameters passed to the Advanced Filter Builder. |
| `advanced-filter-params` | `advancedFilterParams` | object |  |  | Customise the parameters passed to the Advanced Filter |
| `advanced-filter-parent` | `advancedFilterParent` | object |  |  | DOM element to use as the parent for the Advanced Filter to allow it to appear outside of the grid. |
| `agg-funcs` | `aggFuncs` | object | yes |  | A map of 'function name' to 'function' for custom aggregation functions. |
| `aggregate-only-changed-columns` | `aggregateOnlyChangedColumns` | boolean |  |  | When using change detection, only the updated column will be re-aggregated. |
| `aligned-grids` | `alignedGrids` | function |  |  | A list of grids to treat as Aligned Grids. |
| `allow-context-menu-with-control-key` | `allowContextMenuWithControlKey` | boolean |  |  | Allows context menu to show, even when `Ctrl` key is held down. |
| `allow-drag-from-columns-tool-panel` | `allowDragFromColumnsToolPanel` | boolean |  |  | Allow reordering and pinning columns by dragging columns from the Columns Tool Panel to the grid. |
| `allow-show-change-after-filter` | `allowShowChangeAfterFilter` | boolean | yes |  | Set to `true` to have cells flash after data changes even when the change is due to filtering. |
| `always-aggregate-at-root-level` | `alwaysAggregateAtRootLevel` | boolean |  |  | When using aggregations, the grid will always calculate the root level aggregation value. |
| `always-multi-sort` | `alwaysMultiSort` | boolean |  |  | Set to `true` to always multi-sort when the user clicks a column header, regardless of key presses. |
| `always-pass-filter` | `alwaysPassFilter` | object |  |  | Allows rows to always be displayed, even if they don't match the applied filtering. |
| `always-show-horizontal-scroll` | `alwaysShowHorizontalScroll` | boolean |  |  | Set to `true` to always show the horizontal scrollbar. |
| `always-show-vertical-scroll` | `alwaysShowVerticalScroll` | boolean |  |  | Set to `true` to always show the vertical scrollbar. |
| `animate-column-resizing` | `animateColumnResizing` | boolean |  |  | Set to `true` to animate changes to column width when auto-sizing the columns. |
| `animate-rows` | `animateRows` | boolean |  |  | Set to `false` to disable Row Animation which is enabled by default. |
| `apply-quick-filter-before-pivot-or-agg` | `applyQuickFilterBeforePivotOrAgg` | boolean |  |  | When pivoting, Quick Filter is only applied on the pivoted data |
| `async-transaction-wait-millis` | `asyncTransactionWaitMillis` | number |  |  | How many milliseconds to wait before executing a batch of async transactions. |
| `auto-generate-column-defs` | `autoGenerateColumnDefs` | object |  |  | When enabled, column definitions are generated automatically from the first row of `rowData` whenever row data is set or updated. |
| `auto-group-column-def` | `autoGroupColumnDef` | object |  |  | Allows specifying the group 'auto column' if you are not happy with the default. If grouping, this column definition is included as the first column in the grid. If not grouping, this column is not included. |
| `auto-size-padding` | `autoSizePadding` | number |  |  | Number of pixels to add to a column width after the [auto-sizing](./column-sizing/#auto-size-columns-to-fit-cell-contents) calculation. |
| `auto-size-strategy` | `autoSizeStrategy` | object | yes |  | Auto-size the columns when the grid is loaded. Can size to fit the grid width, fit a provided width, or fit the cell contents. |
| `block-load-debounce-millis` | `blockLoadDebounceMillis` | number | yes |  | How many milliseconds to wait before loading a block. Useful when scrolling over many blocks, as it prevents blocks loading until scrolling has settled. |
| `cache-block-size` | `cacheBlockSize` | number |  |  | How many rows for each block in the store, i.e. how many rows returned from the server at a time. |
| `cache-overflow-size` | `cacheOverflowSize` | number | yes |  | How many extra blank rows to display to the user at the end of the dataset, which sets the vertical scroll and then allows the grid to request viewing more rows of data. |
| `cache-quick-filter` | `cacheQuickFilter` | boolean | yes |  | Set to `true` to turn on the Quick Filter cache, used to improve performance when using the Quick Filter. |
| `calculated-columns` | `calculatedColumns` | object |  |  | Enables and configures Calculated Columns. |
| `cell-fade-duration` | `cellFadeDuration` | number |  |  | Sets the duration in milliseconds of how long the "flashed" state animation takes to fade away after the timer set by `cellFlashDuration` has completed. |
| `cell-flash-duration` | `cellFlashDuration` | number |  |  | Sets the duration in milliseconds of how long a cell should remain in its "flashed" state. |
| `cell-selection` | `cellSelection` | object |  |  | Configure cell selection. |
| `chart-menu-items` | `chartMenuItems` | object |  |  | Get chart menu items. Only applies when using AG Charts Enterprise. |
| `chart-theme-overrides` | `chartThemeOverrides` | object | yes |  | Chart theme overrides applied to all themes. |
| `chart-themes` | `chartThemes` | array | yes |  | The list of chart themes that a user can choose from in the chart panel. |
| `chart-tool-panels-def` | `chartToolPanelsDef` | object | yes |  | Allows customisation of the Chart Tool Panels, such as changing the tool panels visibility and order, as well as choosing which charts should be displayed in the chart panel. |
| `clipboard-delimiter` | `clipboardDelimiter` | string |  |  | Specify the delimiter to use when copying to clipboard. |
| `col-resize-default` | `colResizeDefault` | object |  |  | Set to `'shift'` to have shift-resize as the default resize operation (same as user holding down `Shift` while resizing). |
| `column-defs` | `columnDefs` | array |  |  | Array of Column / Column Group definitions. |
| `column-hover-highlight` | `columnHoverHighlight` | boolean |  |  | Set to `true` to highlight columns by adding the `ag-column-hover` CSS class. |
| `column-menu` | `columnMenu` | object | yes |  | Changes the display type of the column menu. |
| `column-types` | `columnTypes` | object |  |  | An object map of custom column types which contain groups of properties that column definitions can reuse by referencing in their `type` property. |
| `components` | `components` | object | yes |  | A map of component names to components. |
| `context` | `context` | any | yes |  | Provides a context object that is provided to different callbacks the grid uses. Used for passing additional information to the callbacks used by your application. |
| `copy-group-headers-to-clipboard` | `copyGroupHeadersToClipboard` | boolean |  |  | Set to `true` to also include group headers when copying to clipboard using `Ctrl + C` clipboard. |
| `copy-headers-to-clipboard` | `copyHeadersToClipboard` | boolean |  |  | Set to `true` to also include headers when copying to clipboard using `Ctrl + C` clipboard. |
| `create-chart-container` | `createChartContainer` | object | yes |  | Callback to enable displaying the chart in an alternative chart container. |
| `custom-chart-themes` | `customChartThemes` | object | yes |  | A map containing custom chart themes. |
| `data-type-definitions` | `dataTypeDefinitions` | object |  |  | An object map of cell data types to their definitions. |
| `datasource` | `datasource` | object |  |  | Provide the datasource for infinite scrolling. |
| `debounce-vertical-scrollbar` | `debounceVerticalScrollbar` | boolean | yes |  | Set to `true` to debounce the vertical scrollbar. Can provide smoother scrolling on slow machines. |
| `debug` | `debug` | boolean | yes |  | Set this to `true` to enable debug information from the grid and related components. Will result in additional logging being output, but very useful when investigating problems. |
| `default-col-def` | `defaultColDef` | object |  |  | A default column definition. Items defined in the actual column definitions get precedence. |
| `default-col-group-def` | `defaultColGroupDef` | object | yes |  | A default column group definition. All column group definitions will use these properties. Items defined in the actual column group definition get precedence. |
| `default-csv-export-params` | `defaultCsvExportParams` | object |  |  | A default configuration object used to export to CSV. |
| `default-excel-export-params` | `defaultExcelExportParams` | object |  |  | A default configuration object used to export to Excel. |
| `delta-sort` | `deltaSort` | boolean |  |  | When enabled, sorts only the rows added/updated by a transaction. |
| `detail-cell-renderer` | `detailCellRenderer` | any |  |  | Provide a custom `detailCellRenderer` to use when a master row is expanded. |
| `detail-cell-renderer-params` | `detailCellRendererParams` | any |  |  | Specifies the params to be used by the Detail Cell Renderer. Can also be a function that provides the params to enable dynamic definitions of the params. |
| `detail-row-auto-height` | `detailRowAutoHeight` | boolean | yes |  | Set to `true` to have the detail grid dynamically change it's height to fit it's rows. |
| `detail-row-height` | `detailRowHeight` | number | yes |  | Set fixed height in pixels for each detail row. |
| `does-external-filter-pass` | `doesExternalFilterPass` | object |  |  | Should return `true` if external filter passes, otherwise `false`. |
| `dom-layout` | `domLayout` | object |  |  | Switch between layout options: `normal`, `autoHeight`, `print`. |
| `drag-and-drop-image-component` | `dragAndDropImageComponent` | any | yes |  | Provide a custom drag and drop image component. |
| `drag-and-drop-image-component-params` | `dragAndDropImageComponentParams` | any |  |  | Customise the parameters provided to the Drag and Drop Image Component. |
| `edit-type` | `editType` | object |  |  | Set to `'fullRow'` to enable Full Row Editing. Otherwise leave blank to edit one cell at a time. |
| `embed-full-width-rows` | `embedFullWidthRows` | boolean |  |  | Set to `true` to have the Full Width Rows embedded in grid's main container so they can be scrolled horizontally. |
| `enable-advanced-filter` | `enableAdvancedFilter` | boolean |  |  | Set to true to enable the Advanced Filter. |
| `enable-browser-tooltips` | `enableBrowserTooltips` | boolean | yes |  | Set to `true` to use the browser's default tooltip instead of using the grid's Tooltip Component. |
| `enable-cell-editing-on-backspace` | `enableCellEditingOnBackspace` | boolean |  |  | Forces Cell Editing to start when backspace is pressed. This is only relevant for MacOS users. |
| `enable-cell-expressions` | `enableCellExpressions` | boolean | yes |  | Set to `true` to allow cell expressions. |
| `enable-cell-span` | `enableCellSpan` | boolean | yes |  | When `true`, enables the cell span feature allowing for the use of the `colDef.spanRows` property. |
| `enable-cell-text-selection` | `enableCellTextSelection` | boolean |  |  | Set to `true` to be able to select the text within cells. |
| `enable-charts` | `enableCharts` | boolean |  |  | Set to `true` to Enable Charts. |
| `enable-fill-handle` | `enableFillHandle` | boolean |  | v32.2 Use `cellSelection.handle` instead | Set to `true` to enable the Fill Handle. |
| `enable-filter-handlers` | `enableFilterHandlers` | boolean | yes |  | Enable filter handlers for custom filter components. |
| `enable-group-edit` | `enableGroupEdit` | boolean | yes |  |  |
| `enable-range-handle` | `enableRangeHandle` | boolean |  | v32.2 Use `cellSelection.handle` instead | Set to `true` to enable the Range Handle. |
| `enable-range-selection` | `enableRangeSelection` | boolean |  | v32.2 Use `cellSelection = true` instead | Set to `true` to enable Range Selection. |
| `enable-row-pinning` | `enableRowPinning` | object |  |  | Determines whether manual row pinning is enabled via the row context menu. |
| `enable-rtl` | `enableRtl` | boolean | yes |  | Set to `true` to operate the grid in RTL (Right to Left) mode. |
| `enable-strict-pivot-column-order` | `enableStrictPivotColumnOrder` | boolean |  |  | Resets pivot column order when impacted by filters, data or configuration changes |
| `ensure-dom-order` | `ensureDomOrder` | boolean | yes |  | When `true`, the order of rows and columns in the DOM are consistent with what is on screen. |
| `enter-navigates-vertically` | `enterNavigatesVertically` | boolean |  |  | Set to `true` along with `enterNavigatesVerticallyAfterEdit` to have Excel-style behaviour for the `Enter` key. |
| `enter-navigates-vertically-after-edit` | `enterNavigatesVerticallyAfterEdit` | boolean |  |  | Set to `true` along with `enterNavigatesVertically` to have Excel-style behaviour for the 'Enter' key. |
| `excel-styles` | `excelStyles` | array | yes |  | A list (array) of Excel styles to be used when exporting to Excel with styles. |
| `exclude-children-when-tree-data-filtering` | `excludeChildrenWhenTreeDataFiltering` | boolean |  |  | Set to `true` to override the default tree data filtering behaviour to instead exclude child nodes from filter results. |
| `fill-handle-direction` | `fillHandleDirection` | object |  | v32.2 Use `cellSelection.handle.direction` instead | Set to `'x'` to force the fill handle direction to horizontal, or set to `'y'` to force the fill handle direction to vertical. |
| `fill-operation` | `fillOperation` | object |  | v32.2 Use `cellSelection.handle.setFillValue` instead | Callback to fill values instead of simply copying values or increasing number values using linear progression. |
| `filter-handlers` | `filterHandlers` | object | yes |  | A map of filter handler key to filter handler function. |
| `find-options` | `findOptions` | object |  |  | Options for the Find feature. |
| `find-search-value` | `findSearchValue` | string |  |  | Text to find within the grid. |
| `floating-filters-height` | `floatingFiltersHeight` | number |  |  | The height in pixels for the row containing the floating filters. If not specified, it uses the theme value of `header-height`. |
| `focus-grid-inner-element` | `focusGridInnerElement` | object |  |  | Allows overriding the element that will be focused when the grid receives focus from outside elements (tabbing into the grid). |
| `formula-data-source` | `formulaDataSource` | object | yes |  | Provide a data source to control where formulas are stored and retrieved. |
| `formula-funcs` | `formulaFuncs` | object | yes |  | A map of 'function name' to 'function' for custom functions that are used for formulas. |
| `full-width-cell-renderer` | `fullWidthCellRenderer` | any |  |  | Provide your own cell renderer component to use for full width rows. |
| `full-width-cell-renderer-params` | `fullWidthCellRendererParams` | any |  |  | Customise the parameters provided to the `fullWidthCellRenderer` component. |
| `functions-read-only` | `functionsReadOnly` | boolean |  |  | If `true`, then row group, pivot and value aggregation will be read-only from the GUI. The grid will display what values are used for each, but will not allow the user to change the selection. |
| `get-business-key-for-node` | `getBusinessKeyForNode` | object |  |  | Return a business key for the node. If implemented, each row in the DOM will have an attribute `row-business-key='abc'` where `abc` is what you return as the business key. |
| `get-chart-toolbar-items` | `getChartToolbarItems` | object | yes |  | Callback to be used to customise the chart toolbar items. |
| `get-child-count` | `getChildCount` | object | yes |  | Allows setting the child count for a group row. |
| `get-context-menu-items` | `getContextMenuItems` | object |  |  | For customising the context menu. |
| `get-data-path` | `getDataPath` | object | yes |  | Callback to be used when working with Tree Data when `treeData = true`. |
| `get-document` | `getDocument` | object |  |  | Allows overriding what `document` is used. Currently used by Drag and Drop (may extend to other places in the future). Use this when you want the grid to use a different `document` than the one available on the global scope. This can happen if docking out components (something which Electron supports) |
| `get-full-row-edit-validation-errors` | `getFullRowEditValidationErrors` | object |  |  | Validates the Full Row Edit. Only relevant when `editType="fullRow"`. |
| `get-group-row-agg` | `getGroupRowAgg` | object |  |  | Callback to use when you need access to more then the current column for aggregation. |
| `get-locale-text` | `getLocaleText` | object | yes |  | A callback for localising text within the grid. |
| `get-main-menu-items` | `getMainMenuItems` | object | yes |  | For customising the main 'column header' menu. |
| `get-row-class` | `getRowClass` | object |  |  | Callback version of property `rowClass` to set class(es) for each row individually. Function should return either a string (class name), array of strings (array of class names) or undefined for no class. |
| `get-row-height` | `getRowHeight` | object |  |  | Callback version of property `rowHeight` to set height for each row individually. Function should return a positive number of pixels, or return `null`/`undefined` to use the default row height. |
| `get-row-id` | `getRowId` | object | yes |  | Provide a pure function that returns a string ID to uniquely identify a given row. This enables the grid to work optimally with data changes and updates. |
| `get-row-style` | `getRowStyle` | object |  |  | Callback version of property `rowStyle` to set style for each row individually. Function should return an object of CSS values or undefined for no styles. |
| `get-server-side-group-key` | `getServerSideGroupKey` | object |  |  | SSRM Tree Data: Allows specifying group keys. |
| `get-server-side-group-level-params` | `getServerSideGroupLevelParams` | object | yes |  | Allows providing different params for different levels of grouping. |
| `grand-total-row` | `grandTotalRow` | object |  |  | When provided, an extra grand total row will be inserted into the grid at the specified position. |
| `grid-id` | `gridId` | string | yes |  | Provide a custom `gridId` for this instance of the grid. Value will be set on the root DOM node using the attribute `grid-id` as well as being accessible via the `gridApi.getGridId()` method. |
| `group-agg-filtering` | `groupAggFiltering` | object |  |  | Set to determine whether filters should be applied on aggregated group values. |
| `group-allow-unbalanced` | `groupAllowUnbalanced` | boolean |  |  | Set to `true` to prevent the grid from creating a '(Blanks)' group for nodes which do not belong to a group, and display the unbalanced nodes alongside group nodes. |
| `group-default-expanded` | `groupDefaultExpanded` | number |  |  | If grouping, set to the number of levels to expand by default, e.g. `0` for none, `1` for first level only, etc. Set to `-1` to expand everything. |
| `group-display-type` | `groupDisplayType` | object |  |  | Specifies how the results of row grouping should be displayed. |
| `group-header-height` | `groupHeaderHeight` | number |  |  | The height in pixels for the rows containing header column groups. If not specified, it uses `headerHeight`. |
| `group-hide-columns-until-expanded` | `groupHideColumnsUntilExpanded` | boolean |  |  | When using `groupDisplayType='multipleColumns'` or `groupHideOpenParents=true`, hides group columns for levels |
| `group-hide-open-parents` | `groupHideOpenParents` | boolean |  |  | Set to `true` to hide parents that are open. When used with multiple columns for showing groups, it can give a more pleasing user experience. |
| `group-hide-parent-of-single-child` | `groupHideParentOfSingleChild` | object |  |  | Enable to display the child row in place of the group row when the group only has a single child. |
| `group-hierarchy-config` | `groupHierarchyConfig` | object |  |  | Custom group hierarchy components can be defined here for later use in `colDef.groupHierarchy` |
| `group-lock-group-columns` | `groupLockGroupColumns` | number | yes |  | If grouping, locks the group settings of a number of columns, e.g. `0` for no group locking. `1` for first group column locked, `-1` for all group columns locked. |
| `group-maintain-order` | `groupMaintainOrder` | boolean |  |  | When `true`, sorting on non-group columns does not reorder groups; only the rows within |
| `group-remove-lowest-single-children` | `groupRemoveLowestSingleChildren` | boolean |  | v33.0.0 - use `groupHideParentOfSingleChild: 'leafGroupsOnly'` instead. | Set to `true` to collapse lowest level groups that only have one child. |
| `group-remove-single-children` | `groupRemoveSingleChildren` | boolean |  | v33.0.0 - use `groupHideParentOfSingleChild` instead. | Set to `true` to collapse groups that only have one child. |
| `group-row-renderer` | `groupRowRenderer` | any |  |  | Provide the Cell Renderer to use when `groupDisplayType = 'groupRows'`. |
| `group-row-renderer-params` | `groupRowRendererParams` | any |  |  | Customise the parameters provided to the `groupRowRenderer` component. |
| `group-selects-children` | `groupSelectsChildren` | boolean |  | v32.2 Use `rowSelection.groupSelects` instead | When `true`, if you select a group, the children of the group will also be selected. |
| `group-selects-filtered` | `groupSelectsFiltered` | boolean |  | v32.2 Use `rowSelection.groupSelects` instead | If using `groupSelectsChildren`, then only the children that pass the current filter will get selected. |
| `group-suppress-blank-header` | `groupSuppressBlankHeader` | boolean |  |  | If `true`, and showing footer, aggregate data will always be displayed at both the header and footer levels. This stops the possibly undesirable behaviour of the header details 'jumping' to the footer on expand. |
| `group-total-row` | `groupTotalRow` | object |  |  | When provided, an extra row group total row will be inserted into row groups at the specified position, to display |
| `header-height` | `headerHeight` | number |  |  | The height in pixels for the row containing the column label header. If not specified, it uses the theme value of `header-height`. |
| `hide-padded-header-rows` | `hidePaddedHeaderRows` | boolean |  |  | Hide any column header rows that would only contain padded groups. |
| `icons` | `icons` | object | yes |  | Icons to use inside the grid instead of the grid's default icons. |
| `include-hidden-columns-in-advanced-filter` | `includeHiddenColumnsInAdvancedFilter` | boolean |  |  | Hidden columns are excluded from the Advanced Filter by default. |
| `include-hidden-columns-in-quick-filter` | `includeHiddenColumnsInQuickFilter` | boolean |  |  | Hidden columns are excluded from the Quick Filter by default. |
| `infinite-initial-row-count` | `infiniteInitialRowCount` | number | yes |  | How many extra blank rows to display to the user at the end of the dataset, which sets the vertical scroll and then allows the grid to request viewing more rows of data. |
| `initial-group-order-comparator` | `initialGroupOrderComparator` | object |  |  | Allows default sorting of groups. |
| `initial-state` | `initialState` | object | yes |  | Initial state for the grid. Only read once on initialization. Can be used in conjunction with `api.getState()` to save and restore grid state. |
| `invalid-edit-value-mode` | `invalidEditValueMode` | object |  |  | Set to `block` to block the commit of invalid cell edits, keeping editors open. |
| `is-apply-server-side-transaction` | `isApplyServerSideTransaction` | object |  |  | Allows cancelling transactions. |
| `is-external-filter-present` | `isExternalFilterPresent` | object |  |  | Grid calls this method to know if an external filter is present. |
| `is-full-width-row` | `isFullWidthRow` | object |  |  | Tells the grid if this row should be rendered as full width. |
| `is-group-open-by-default` | `isGroupOpenByDefault` | object |  |  | (Client-side Row Model only) Allows groups to be open by default. |
| `is-row-master` | `isRowMaster` | object |  |  | Callback to be used with Master Detail to determine if a row should be a master row. If `false` is returned no detail row will exist for this row. |
| `is-row-pinnable` | `isRowPinnable` | object |  |  | Return `true` if the grid should allow the row to be manually pinned. |
| `is-row-pinned` | `isRowPinned` | object |  |  | Called for every row in the grid. |
| `is-row-selectable` | `isRowSelectable` | object |  | v32.2 Use `rowSelection.isRowSelectable` instead | Callback to be used to determine which rows are selectable. By default rows are selectable, so return `false` to make a row un-selectable. |
| `is-row-valid-drop-position` | `isRowValidDropPosition` | object |  |  | Called by drag and drop when rows are dragged over another row to conditionally prevent dropping the dragged row on the hovered row. |
| `is-server-side-group` | `isServerSideGroup` | object |  |  | SSRM Tree Data: Allows specifying which rows are expandable. |
| `is-server-side-group-open-by-default` | `isServerSideGroupOpenByDefault` | object |  |  | Allows groups to be open by default. |
| `keep-detail-rows` | `keepDetailRows` | boolean | yes |  | Set to `true` to keep detail rows for when they are displayed again. |
| `keep-detail-rows-count` | `keepDetailRowsCount` | number | yes |  | Sets the number of details rows to keep. |
| `load-theme-google-fonts` | `loadThemeGoogleFonts` | boolean |  |  | If your theme uses a font that is available on Google Fonts, pass true to load it from Google's CDN. |
| `loading` | `loading` | boolean |  |  | Show or hide the loading overlay. |
| `loading-cell-renderer` | `loadingCellRenderer` | any |  |  | Provide your own loading cell renderer to use when data is loading via a DataSource or when a cell renderer is deferred. |
| `loading-cell-renderer-params` | `loadingCellRendererParams` | any |  |  | Params to be passed to the `loadingCellRenderer` component. |
| `loading-cell-renderer-selector` | `loadingCellRendererSelector` | object | yes |  | Callback to select which loading cell renderer to be used when data is loading via a DataSource or when a cell renderer is deferred. |
| `loading-overlay-component` | `loadingOverlayComponent` | any |  |  | Provide a custom loading overlay component. |
| `loading-overlay-component-params` | `loadingOverlayComponentParams` | any |  |  | Customise the parameters provided to the loading overlay component. |
| `locale-text` | `localeText` | object | yes |  | A map of key->value pairs for localising text within the grid. |
| `maintain-column-order` | `maintainColumnOrder` | boolean |  |  | Keeps the order of Columns maintained after new Column Definitions are updated. |
| `master-detail` | `masterDetail` | boolean |  |  | Set to `true` to enable Master Detail. |
| `max-blocks-in-cache` | `maxBlocksInCache` | number | yes |  | How many blocks to keep in the store. Default is no limit, so every requested block is kept. Use this if you have memory concerns, and blocks that were least recently viewed will be purged when the limit is hit. The grid will additionally make sure it has all the blocks needed to display what is currently visible, in case this property is set to a low value. |
| `max-concurrent-datasource-requests` | `maxConcurrentDatasourceRequests` | number | yes |  | How many requests to hit the server with concurrently. If the max is reached, requests are queued. |
| `multi-sort-key` | `multiSortKey` | object |  |  | Set to `'ctrl'` to have multi sorting by clicking work using the `Ctrl` (or `Command ⌘` for Mac) key. |
| `navigate-to-next-cell` | `navigateToNextCell` | object |  |  | Allows overriding the default behaviour for when user hits navigation (arrow) key when a cell is focused. Return the next Cell position to navigate to or `null` to stay on current cell. |
| `navigate-to-next-header` | `navigateToNextHeader` | object |  |  | Allows overriding the default behaviour for when user hits navigation (arrow) key when a header is focused. Return the next Header position to navigate to or `null` to stay on current header. |
| `no-rows-overlay-component` | `noRowsOverlayComponent` | any |  |  | Provide a custom no-rows overlay component. |
| `no-rows-overlay-component-params` | `noRowsOverlayComponentParams` | any |  |  | Customise the parameters provided to the no-rows overlay component. |
| `note-hide-delay` | `noteHideDelay` | number |  |  | The delay in milliseconds before a note is hidden after the pointer leaves a noted cell or note popup. |
| `note-show-delay` | `noteShowDelay` | number |  |  | The delay in milliseconds before a note is shown when hovering a noted cell. |
| `note-trigger` | `noteTrigger` | object |  |  | Changes how existing notes are opened. |
| `notes-data-source` | `notesDataSource` | object |  |  | Provide a data source to control where notes are stored and retrieved. |
| `overlay-component` | `overlayComponent` | any | yes |  | Provide a custom overlay component to be used for all grid provided overlays (loading, no rows, no matching rows, exporting etc). |
| `overlay-component-params` | `overlayComponentParams` | any |  |  | Customise the parameters provided to the `overlayComponent`. |
| `overlay-component-selector` | `overlayComponentSelector` | object | yes |  | Callback to dynamically provide a custom overlay component complete with custom params based on the selector params. |
| `overlay-loading-template` | `overlayLoadingTemplate` | string |  |  | Provide a HTML string to override the default loading overlay. Supports non-empty plain text or HTML with a single root element. |
| `overlay-no-rows-template` | `overlayNoRowsTemplate` | string |  |  | Provide a HTML string to override the default no-rows overlay. Supports non-empty plain text or HTML with a single root element. |
| `paginate-child-rows` | `paginateChildRows` | boolean | yes |  | Set to `true` to have pages split children of groups when using Row Grouping or detail rows with Master Detail. |
| `pagination` | `pagination` | boolean |  |  | Set whether pagination is enabled. |
| `pagination-auto-page-size` | `paginationAutoPageSize` | boolean |  |  | Set to `true` so that the number of rows to load per page is automatically adjusted by the grid so each page shows enough rows to just fill the area designated for the grid. If `false`, `paginationPageSize` is used. |
| `pagination-number-formatter` | `paginationNumberFormatter` | object | yes |  | Allows user to format the numbers in the pagination panel, i.e. 'row count' and 'page number' labels. This is for pagination panel only, to format numbers inside the grid's cells (i.e. your data), then use `valueFormatter` in the column definitions. |
| `pagination-page-size` | `paginationPageSize` | number |  |  | How many rows to load per page. If `paginationAutoPageSize` is specified, this property is ignored. |
| `pagination-page-size-selector` | `paginationPageSizeSelector` | array |  |  | Determines if the page size selector is shown in the pagination panel or not. |
| `pagination-panels` | `paginationPanels` | array |  |  | Controls which built-in components appear in the pagination panel and in what order. |
| `pinned-bottom-row-data` | `pinnedBottomRowData` | array |  |  | Data to be displayed as pinned bottom rows in the grid. |
| `pinned-top-row-data` | `pinnedTopRowData` | array |  |  | Data to be displayed as pinned top rows in the grid. |
| `pivot-column-group-totals` | `pivotColumnGroupTotals` | object |  |  | When set and the grid is in pivot mode, automatically calculated totals will appear within the Pivot Column Groups, in the position specified. |
| `pivot-default-expanded` | `pivotDefaultExpanded` | number |  |  | If pivoting, set to the number of column group levels to expand by default, e.g. `0` for none, `1` for first level only, etc. Set to `-1` to expand everything. |
| `pivot-group-header-height` | `pivotGroupHeaderHeight` | number |  |  | The height in pixels for the row containing header column groups when in pivot mode. If not specified, it uses `groupHeaderHeight`. |
| `pivot-header-height` | `pivotHeaderHeight` | number |  |  | The height in pixels for the row containing the columns when in pivot mode. If not specified, it uses `headerHeight`. |
| `pivot-max-generated-columns` | `pivotMaxGeneratedColumns` | number |  |  | The maximum number of generated columns before the grid halts execution. Upon reaching this number, the grid halts generation of columns |
| `pivot-mode` | `pivotMode` | boolean |  |  | Set to `true` to enable pivot mode. |
| `pivot-panel-show` | `pivotPanelShow` | object | yes |  | When to show the 'pivot panel' (where you drag rows to pivot) at the top. Note that the pivot panel will never show if `pivotMode` is off. |
| `pivot-row-totals` | `pivotRowTotals` | object |  |  | When set and the grid is in pivot mode, automatically calculated totals will appear for each value column in the position specified. |
| `pivot-suppress-auto-column` | `pivotSuppressAutoColumn` | boolean | yes |  | If `true`, the grid will not swap in the grouping column when pivoting. Useful if pivoting using Server Side Row Model or Viewport Row Model and you want full control of all columns including the group column. |
| `popup-parent` | `popupParent` | object |  |  | DOM element to use as the popup parent for grid popups (context menu, column menu etc). |
| `post-process-popup` | `postProcessPopup` | object |  |  | Allows user to process popups after they are created. Applications can use this if they want to, for example, reposition the popup. |
| `post-sort-rows` | `postSortRows` | object |  |  | Callback to perform additional sorting after the grid has sorted the rows. |
| `prevent-default-on-context-menu` | `preventDefaultOnContextMenu` | boolean |  |  | When using `suppressContextMenu`, you can use the `onCellContextMenu` function to provide your own code to handle cell `contextmenu` events. |
| `process-auto-generated-column-defs` | `processAutoGeneratedColumnDefs` | object |  |  | Callback fired after auto-generating column definitions and before they are applied to the grid. |
| `process-cell-for-clipboard` | `processCellForClipboard` | object |  |  | Allows you to process cells for the clipboard. Handy if for example you have `Date` objects that need to have a particular format if importing into Excel. |
| `process-cell-from-clipboard` | `processCellFromClipboard` | object |  |  | Allows you to process cells from the clipboard. Handy if for example you have number fields and want to block non-numbers from getting into the grid. |
| `process-data-from-clipboard` | `processDataFromClipboard` | object |  |  | Allows complete control of the paste operation, including cancelling the operation (so nothing happens) or replacing the data with other data. |
| `process-file-input` | `processFileInput` | function |  |  | Callback to handle files received via the file input overlay (drag-and-drop or file browser). |
| `process-group-header-for-clipboard` | `processGroupHeaderForClipboard` | object |  |  | Allows you to process group header values for the clipboard. |
| `process-header-for-clipboard` | `processHeaderForClipboard` | object |  |  | Allows you to process header values for the clipboard. |
| `process-pivot-result-col-def` | `processPivotResultColDef` | object |  |  | Callback for the mutation of the generated pivot result column definitions |
| `process-pivot-result-col-group-def` | `processPivotResultColGroupDef` | object |  |  | Callback for the mutation of the generated pivot result column group definitions |
| `process-row-post-create` | `processRowPostCreate` | object |  |  | Callback fired after the row is rendered into the DOM. Should not be used to initiate side effects. |
| `process-unpinned-columns` | `processUnpinnedColumns` | object | yes |  | Allows the user to process the columns being removed from the pinned section because the viewport is too small to accommodate them. |
| `purge-closed-row-nodes` | `purgeClosedRowNodes` | boolean |  |  | When enabled, closing group rows will remove children of that row. Next time the row is opened, child rows will be read from the datasource again. This property only applies when there is Row Grouping or Tree Data. |
| `quick-filter-matcher` | `quickFilterMatcher` | object |  |  | Changes the matching logic for whether a row passes the Quick Filter. |
| `quick-filter-parser` | `quickFilterParser` | object |  |  | Changes how the Quick Filter splits the Quick Filter text into search terms. |
| `quick-filter-text` | `quickFilterText` | string |  |  | Rows are filtered using this text as a Quick Filter. |
| `reactive-custom-components` | `reactiveCustomComponents` | boolean | yes | As of v32 custom components are created reactively by default. Set this property to `false` to switch to the legacy way of declaring custom components imperatively. | **React only**. |
| `read-only-edit` | `readOnlyEdit` | boolean |  |  | Set to `true` to stop the grid updating data after `Edit`, `Clipboard` and `Fill Handle` operations. When this is set, it is intended the application will update the data, eg in an external immutable store, and then pass the new dataset to the grid. <br />**Note:** `rowNode.setDataValue()` does not update the value of the cell when this is `True`, it fires `onCellEditRequest` instead. |
| `refresh-after-group-edit` | `refreshAfterGroupEdit` | boolean |  |  | When `true`, the grid re-evaluates the grouping hierarchy after editing a grouped column value, |
| `remove-pivot-header-row-when-single-value-column` | `removePivotHeaderRowWhenSingleValueColumn` | boolean |  |  | Set to `true` to omit the value Column header when there is only a single value column. |
| `rendering-mode` | `renderingMode` | object |  |  | ** React only**. |
| `reset-row-data-on-update` | `resetRowDataOnUpdate` | boolean |  |  | When enabled, getRowId() callback is implemented and new Row Data is set, the grid will disregard all previous rows and treat the new Row Data as new data. As a consequence, all Row State (eg selection, rendered rows) will be reset. |
| `row-buffer` | `rowBuffer` | number |  |  | The number of rows rendered outside the viewable area the grid renders. |
| `row-class` | `rowClass` | array |  |  | CSS class(es) for all rows. Provide either a string (class name) or array of strings (array of class names). |
| `row-class-rules` | `rowClassRules` | object |  |  | Rules which can be applied to include certain CSS classes. |
| `row-data` | `rowData` | array |  |  | Set the data to be displayed as rows in the grid. |
| `row-drag-entire-row` | `rowDragEntireRow` | boolean |  |  | Set to `true` to enable clicking and dragging anywhere on the row without the need for a drag handle. |
| `row-drag-insert-delay` | `rowDragInsertDelay` | number |  |  | Used if rowDragManaged is enabled and treeData is enabled, |
| `row-drag-managed` | `rowDragManaged` | boolean |  |  | Set to `true` to enable Managed Row Dragging. |
| `row-drag-multi-row` | `rowDragMultiRow` | boolean |  |  | Set to `true` to enable dragging multiple rows at the same time. |
| `row-drag-text` | `rowDragText` | object | yes |  | A callback that should return a string to be displayed by the `rowDragComp` while dragging a row. |
| `row-group-panel-show` | `rowGroupPanelShow` | object |  |  | When to show the 'row group panel' (where you drag rows to group) at the top. |
| `row-group-panel-suppress-sort` | `rowGroupPanelSuppressSort` | boolean |  |  | Set to `true` to suppress sort indicators and actions from the row group panel. |
| `row-height` | `rowHeight` | number |  |  | Default row height in pixels. |
| `row-model-type` | `rowModelType` | object | yes |  | Sets the row model type. |
| `row-multi-select-with-click` | `rowMultiSelectWithClick` | boolean |  | v32.2 Use `rowSelection.enableSelectionWithoutKeys` instead | Set to `true` to allow multiple rows to be selected using single click. |
| `row-numbers` | `rowNumbers` | object |  |  | Configure the Row Numbers Feature. |
| `row-selection` | `rowSelection` | object |  |  | Use the `RowSelectionOptions` object to configure row selection. The string values `'single'` and `'multiple'` are deprecated. |
| `row-style` | `rowStyle` | object |  |  | The style properties to apply to all rows. Set to an object of key (style names) and values (style values). |
| `scrollbar-width` | `scrollbarWidth` | number | yes |  | Tell the grid how wide in pixels the scrollbar is, which is used in grid width calculations. Set only if using non-standard browser-provided scrollbars, so the grid can use the non-standard size in its calculations. |
| `selection-column-def` | `selectionColumnDef` | object |  |  | Configure the selection column, used for displaying checkboxes. |
| `send-to-clipboard` | `sendToClipboard` | object |  |  | Allows you to get the data that would otherwise go to the clipboard. To be used when you want to control the 'copy to clipboard' operation yourself. |
| `server-side-datasource` | `serverSideDatasource` | object |  |  | Provide the `serverSideDatasource` for server side row model. |
| `server-side-enable-client-side-sort` | `serverSideEnableClientSideSort` | boolean |  |  | When enabled, sorts fully loaded groups in the browser instead of requesting from the server. |
| `server-side-initial-row-count` | `serverSideInitialRowCount` | number | yes |  | Set how many loading rows to display to the user for the root level group. |
| `server-side-only-refresh-filtered-groups` | `serverSideOnlyRefreshFilteredGroups` | boolean | yes |  | When enabled, only refresh groups directly impacted by a filter. This property only applies when there is Row Grouping & filtering is handled on the server. |
| `server-side-pivot-result-field-separator` | `serverSidePivotResultFieldSeparator` | string | yes |  | Used to split pivot field strings for generating pivot result columns when `pivotResultFields` is provided as part of a `getRows` success. |
| `server-side-sort-all-levels` | `serverSideSortAllLevels` | boolean |  |  | When enabled, always refreshes top level groups regardless of which column was sorted. This property only applies when there is Row Grouping & sorting is handled on the server. |
| `show-opened-group` | `showOpenedGroup` | boolean |  |  | Shows the open group in the group column for non-group rows. |
| `side-bar` | `sideBar` | array |  |  | Specifies the side bar components. |
| `single-click-edit` | `singleClickEdit` | boolean |  |  | Set to `true` to enable Single Click Editing for cells, to start editing with a single click. |
| `skip-header-on-auto-size` | `skipHeaderOnAutoSize` | boolean | yes |  | Set this to `true` to skip the `headerName` when `autoSize` is called by default. |
| `sorting-order` | `sortingOrder` | array |  | v33 Use `defaultColDef.sortingOrder` instead | Array defining the order in which sorting occurs (if sorting is enabled). Values can be `'asc'`, `'desc'` or `null`. For example: `sortingOrder: ['asc', 'desc']`. |
| `ssrm-expand-all-affects-all-rows` | `ssrmExpandAllAffectsAllRows` | boolean |  |  | Controls how expand/collapse operations affect all rows and group interactions. |
| `status-bar` | `statusBar` | object |  |  | Specifies the status bar components to use in the status bar. |
| `stop-editing-when-cells-lose-focus` | `stopEditingWhenCellsLoseFocus` | boolean | yes |  | Set this to `true` to stop cell editing when grid loses focus. |
| `style-nonce` | `styleNonce` | string |  |  | The nonce attribute to set on style elements added to the document by |
| `suppress-advanced-filter-eval` | `suppressAdvancedFilterEval` | boolean |  | As of v34, advanced filter no longer uses function evaluation, so this option has no effect. |  |
| `suppress-agg-filtered-only` | `suppressAggFilteredOnly` | boolean |  |  | Set to `true` so that aggregations are not impacted by filtering. |
| `suppress-agg-func-in-header` | `suppressAggFuncInHeader` | boolean |  |  | When `true`, column headers won't include the `aggFunc` name, e.g. `'sum(Bank Balance)`' will just be `'Bank Balance'`. |
| `suppress-animation-frame` | `suppressAnimationFrame` | boolean | yes |  | When `true`, the grid will not use animation frames when drawing rows while scrolling. Use this if and only if the grid is working fast enough on all users machines and you want to avoid the temporarily empty rows. |
| `suppress-auto-size` | `suppressAutoSize` | boolean | yes |  | Suppresses auto-sizing columns for columns. In other words, double clicking a column's header's edge will not auto-size. |
| `suppress-browser-resize-observer` | `suppressBrowserResizeObserver` | boolean | yes | As of v32.2 the grid always uses the browser's ResizeObserver, this grid option has no effect |  |
| `suppress-cell-focus` | `suppressCellFocus` | boolean |  |  | If `true`, cells won't be focusable. This means keyboard navigation will be disabled for grid cells, but remain enabled in other elements of the grid such as column headers, floating filters, tool panels. |
| `suppress-change-detection` | `suppressChangeDetection` | boolean |  |  | Disables change detection. |
| `suppress-clear-on-fill-reduction` | `suppressClearOnFillReduction` | boolean |  | v32.2 Use `cellSelection.suppressClearOnFillReduction` instead | Set this to `true` to prevent cell values from being cleared when the Range Selection is reduced by the Fill Handle. |
| `suppress-click-edit` | `suppressClickEdit` | boolean |  |  | Set to `true` so that neither single nor double click starts editing. |
| `suppress-clipboard-api` | `suppressClipboardApi` | boolean |  |  | Set to `true` to stop the grid trying to use the Clipboard API, if it is blocked, and immediately fallback to the workaround. |
| `suppress-clipboard-paste` | `suppressClipboardPaste` | boolean |  |  | Set to `true` to turn off paste operations within the grid. |
| `suppress-column-move-animation` | `suppressColumnMoveAnimation` | boolean |  |  | If `true`, the `ag-column-moving` class is not added to the grid while columns are moving. In the default themes, this results in no animation when moving columns. |
| `suppress-column-virtualisation` | `suppressColumnVirtualisation` | boolean | yes |  | Set to `true` so that the grid doesn't virtualise the columns. For example, if you have 100 columns, but only 10 visible due to scrolling, all 100 will always be rendered. |
| `suppress-content-visibility-auto` | `suppressContentVisibilityAuto` | boolean | yes |  | Set to `false` to enable `content-visibility: auto` on the grid wrapper element. This improves performance by allowing the browser to skip rendering grids that are off screen, but may cause issues if your application depends on receiving resize events from hidden grids. |
| `suppress-context-menu` | `suppressContextMenu` | boolean |  |  | Set to `true` to not show the context menu. Use if you don't want to use the default 'right click' context menu. |
| `suppress-copy-rows-to-clipboard` | `suppressCopyRowsToClipboard` | boolean |  | v32.2 Use `rowSelection.copySelectedRows` instead. | Set to `true` to copy the cell range or focused cell to the clipboard and never the selected rows. |
| `suppress-copy-single-cell-ranges` | `suppressCopySingleCellRanges` | boolean |  | v32.2 Use `rowSelection.copySelectedRows` instead. | Set to `true` to copy rows instead of ranges when a range with only a single cell is selected. |
| `suppress-csv-export` | `suppressCsvExport` | boolean |  |  | Prevents the user from exporting the grid to CSV. |
| `suppress-cut-to-clipboard` | `suppressCutToClipboard` | boolean |  |  | Set to `true` to block **cut** operations within the grid. |
| `suppress-drag-leave-hides-columns` | `suppressDragLeaveHidesColumns` | boolean |  |  | If `true`, when you drag a column out of the grid (e.g. to the group zone) the column is not hidden. |
| `suppress-excel-export` | `suppressExcelExport` | boolean |  |  | Prevents the user from exporting the grid to Excel. |
| `suppress-expandable-pivot-groups` | `suppressExpandablePivotGroups` | boolean | yes |  | When enabled, pivot column groups will appear 'fixed', without the ability to expand and collapse the column groups. |
| `suppress-field-dot-notation` | `suppressFieldDotNotation` | boolean |  |  | If `true`, then dots in field names (e.g. `'address.firstLine'`) are not treated as deep references. Allows you to use dots in your field name if you prefer. |
| `suppress-focus-after-refresh` | `suppressFocusAfterRefresh` | boolean |  |  | Set to `true` to not set focus back on the grid after a refresh. This can avoid issues where you want to keep the focus on another part of the browser. |
| `suppress-group-changes-column-visibility` | `suppressGroupChangesColumnVisibility` | object |  |  | Enable to prevent column visibility changing when grouped columns are changed. |
| `suppress-group-rows-sticky` | `suppressGroupRowsSticky` | boolean | yes |  | Set to `true` prevent Group Rows from sticking to the top of the grid. |
| `suppress-header-focus` | `suppressHeaderFocus` | boolean |  |  | If `true`, header cells won't be focusable. This means keyboard navigation will be disabled for grid header cells, but remain enabled in other elements of the grid such as grid cells and tool panels. |
| `suppress-horizontal-scroll` | `suppressHorizontalScroll` | boolean |  |  | Set to `true` to never show the horizontal scroll. This is useful if the grid is aligned with another grid and will scroll when the other grid scrolls. (Should not be used in combination with `alwaysShowHorizontalScroll`.) |
| `suppress-last-empty-line-on-paste` | `suppressLastEmptyLineOnPaste` | boolean |  |  | Set to `true` to work around a bug with Excel (Windows) that adds an extra empty line at the end of ranges copied to the clipboard. |
| `suppress-loading-overlay` | `suppressLoadingOverlay` | boolean | yes | v32 - Deprecated. Use `suppressOverlays=['loading']` or `loading=false` instead. | Disables the 'loading' overlay. |
| `suppress-maintain-unsorted-order` | `suppressMaintainUnsortedOrder` | boolean |  |  | Set to `true` to suppress sorting of un-sorted data to match original row data. |
| `suppress-make-column-visible-after-un-group` | `suppressMakeColumnVisibleAfterUnGroup` | boolean |  | v33.0.0 - Use `suppressGroupChangesColumnVisibility: 'suppressShowOnUngroup'` instead. | By default, when a column is un-grouped, i.e. using the Row Group Panel, it is made visible in the grid. This property stops the column becoming visible again when un-grouping. |
| `suppress-max-rendered-row-restriction` | `suppressMaxRenderedRowRestriction` | boolean | yes |  | By default the grid has a limit of rendering a maximum of 500 rows at once (remember the grid only renders rows you can see, so unless your display shows more than 500 rows without vertically scrolling this will never be an issue). |
| `suppress-menu-hide` | `suppressMenuHide` | boolean |  |  | Only recommended for use if `columnMenu = 'legacy'`. |
| `suppress-middle-click-scrolls` | `suppressMiddleClickScrolls` | boolean |  |  | If `true`, middle clicks will result in `click` events for cells and rows. Otherwise the browser will use middle click to scroll the grid.<br />**Note:** Not all browsers fire `click` events with the middle button. Most will fire only `mousedown` and `mouseup` events, which can be used to focus a cell, but will not work to call the `onCellClicked` function. |
| `suppress-model-update-after-update-transaction` | `suppressModelUpdateAfterUpdateTransaction` | boolean |  |  | Prevents Transactions changing sort, filter, group or pivot state when transaction only contains updates. |
| `suppress-movable-columns` | `suppressMovableColumns` | boolean |  |  | Set to `true` to suppress column moving, i.e. to make the columns fixed position. |
| `suppress-move-when-column-dragging` | `suppressMoveWhenColumnDragging` | boolean |  |  | Set to `true` to suppress moving columns while dragging the Column Header. This option highlights the position where the column will be placed and it will only move it on mouse up. |
| `suppress-move-when-row-dragging` | `suppressMoveWhenRowDragging` | boolean |  |  | Set to `true` to suppress moving rows while dragging the `rowDrag` waffle. This option highlights the position where the row will be placed and it will only move the row on mouse up. |
| `suppress-multi-range-selection` | `suppressMultiRangeSelection` | boolean |  | v32.2 Use `cellSelection.suppressMultiRanges` instead | If `true`, only a single range can be selected. |
| `suppress-multi-sort` | `suppressMultiSort` | boolean |  |  | Set to `true` to suppress multi-sort when the user shift-clicks a column header. |
| `suppress-no-rows-overlay` | `suppressNoRowsOverlay` | boolean | yes |  | Set to `true` to prevent the no-rows overlay being shown when there is no row data. |
| `suppress-overlays` | `suppressOverlays` | array |  |  | List of provided overlay names to suppress. One of `loading`, `noRows`, `noMatchingRows`, `exporting`, `fileInput`. |
| `suppress-pagination-panel` | `suppressPaginationPanel` | boolean |  |  | If `true`, the default grid controls for navigation are hidden. |
| `suppress-prevent-default-on-mouse-wheel` | `suppressPreventDefaultOnMouseWheel` | boolean | yes |  | If `true`, mouse wheel events will be passed to the browser. Useful if your grid has no vertical scrolls and you want the mouse to scroll the browser page. |
| `suppress-property-names-check` | `suppressPropertyNamesCheck` | boolean | yes | As of v33 `gridOptions` and `columnDefs` both have a `context` property that should be used for arbitrary user data. This means that column definitions and gridOptions should only contain valid properties making this property redundant. |  |
| `suppress-row-click-selection` | `suppressRowClickSelection` | boolean |  | v32.2 Use `rowSelection.enableClickSelection` instead | If `true`, row selection won't happen when rows are clicked. Use when you only want checkbox selection. |
| `suppress-row-deselection` | `suppressRowDeselection` | boolean |  | v32.2 Use `rowSelection.enableClickSelection` instead | If `true`, rows will not be deselected if you hold down `Ctrl` and click the row or press `Space`. |
| `suppress-row-drag` | `suppressRowDrag` | boolean |  |  | Set to `true` to suppress row dragging. |
| `suppress-row-group-hides-columns` | `suppressRowGroupHidesColumns` | boolean |  | v33.0.0 - Use `suppressGroupChangesColumnVisibility: 'suppressHideOnGroup'` instead. | If `true`, when you drag a column into a row group panel the column is not hidden. |
| `suppress-row-hover-highlight` | `suppressRowHoverHighlight` | boolean |  |  | Set to `true` to not highlight rows by adding the `ag-row-hover` CSS class. |
| `suppress-row-transform` | `suppressRowTransform` | boolean | yes |  | Uses CSS `top` instead of CSS `transform` for positioning rows. Useful if the transform function is causing issues such as used in row spanning. |
| `suppress-row-virtualisation` | `suppressRowVirtualisation` | boolean | yes |  | Set to `true` so that the grid doesn't virtualise the rows. For example, if you have 100 rows, but only 10 visible due to scrolling, all 100 will always be rendered. |
| `suppress-scroll-on-new-data` | `suppressScrollOnNewData` | boolean |  |  | When `true`, the grid will not scroll to the top when new row data is provided. Use this if you don't want the default behaviour of scrolling to the top every time you load new data. |
| `suppress-scroll-when-popups-are-open` | `suppressScrollWhenPopupsAreOpen` | boolean |  |  | When `true`, the grid will not allow mousewheel / touchpad scroll when popup elements are present. |
| `suppress-server-side-full-width-loading-row` | `suppressServerSideFullWidthLoadingRow` | boolean |  |  | When `true`, the Server-side Row Model will not use a full width loading renderer, instead using the colDef `loadingCellRenderer` if present. |
| `suppress-set-filter-by-default` | `suppressSetFilterByDefault` | boolean | yes |  | When using AG Grid Enterprise, the Set Filter is used by default when `filter: true` is set on column definitions. |
| `suppress-start-edit-on-tab` | `suppressStartEditOnTab` | boolean |  |  | Determine the behavior when navigating to the next/previous editable cell. Default is to begin editing the cell. |
| `suppress-sticky-total-row` | `suppressStickyTotalRow` | object |  |  | Suppress the sticky behaviour of the total rows, can be suppressed individually by passing `'grand'` or `'group'`. |
| `suppress-touch` | `suppressTouch` | boolean | yes |  | Disables touch support (but does not remove the browser's efforts to simulate mouse events on touch). |
| `tab-index` | `tabIndex` | number | yes |  | Change this value to set the tabIndex order of the Grid within your application. |
| `tab-to-next-cell` | `tabToNextCell` | object |  |  | Allows overriding the default behaviour for when user hits `Tab` key when a cell is focused. |
| `tab-to-next-grid-container` | `tabToNextGridContainer` | object |  |  | Allows overriding the default behaviour when tabbing between core grid containers. |
| `tab-to-next-header` | `tabToNextHeader` | object |  |  | Allows overriding the default behaviour for when user hits `Tab` key when a header is focused. |
| `theme` | `theme` | object |  |  | Theme to apply to the grid, or the string "legacy" to opt back into the |
| `theme-css-layer` | `themeCssLayer` | string |  |  | The CSS layer that this theme should be rendered onto. When specified, |
| `theme-style-container` | `themeStyleContainer` | function | yes |  | An element to insert style elements into when injecting styles into the |
| `toolbar` | `toolbar` | object |  |  | Specifies the toolbar items to use in the toolbar. |
| `tooltip-hide-delay` | `tooltipHideDelay` | number |  |  | The delay in milliseconds that it takes for tooltips to hide once they have been displayed. |
| `tooltip-interaction` | `tooltipInteraction` | boolean | yes |  | Set to `true` to enable tooltip interaction. When this option is enabled, the tooltip will not hide while the |
| `tooltip-mouse-track` | `tooltipMouseTrack` | boolean | yes |  | Set to `true` to have tooltips follow the cursor once they are displayed. |
| `tooltip-show-delay` | `tooltipShowDelay` | number |  |  | The delay in milliseconds that it takes for tooltips to show up once an element is hovered over. |
| `tooltip-show-mode` | `tooltipShowMode` | object |  |  | This defines when tooltip will show up for Cells, Headers and SetFilter Items. |
| `tooltip-switch-show-delay` | `tooltipSwitchShowDelay` | number |  |  | The delay in milliseconds before a tooltip is shown when moving the pointer from one tooltip-enabled element to |
| `tooltip-trigger` | `tooltipTrigger` | object | yes |  | The trigger that will cause tooltips to show and hide. |
| `tree-data` | `treeData` | boolean |  |  | Set to `true` to enable the Grid to work with Tree Data. |
| `tree-data-children-field` | `treeDataChildrenField` | string |  |  | The name of the field to use in a data item to retrieve the array of children nodes of a node when while using treeData=true. |
| `tree-data-display-type` | `treeDataDisplayType` | object |  |  | Specifies how tree data should be displayed. |
| `tree-data-parent-id-field` | `treeDataParentIdField` | string |  |  | The name of the field to use in a data item to find the parent node of a node when using treeData=true. |
| `un-sort-icon` | `unSortIcon` | boolean |  | v33 Use `defaultColDef.unSortIcon` instead | Set to `true` to show the 'no sort' icon. |
| `undo-redo-cell-editing` | `undoRedoCellEditing` | boolean | yes |  | Set to `true` to enable Undo / Redo while editing. |
| `undo-redo-cell-editing-limit` | `undoRedoCellEditingLimit` | number | yes |  | Set the size of the undo / redo stack. |
| `value-cache` | `valueCache` | boolean | yes |  | Set to `true` to turn on the value cache. |
| `value-cache-never-expires` | `valueCacheNeverExpires` | boolean | yes |  | Set to `true` to configure the value cache to not expire after data updates. |
| `viewport-datasource` | `viewportDatasource` | object |  |  | To use the viewport row model you need to provide the grid with a `viewportDatasource`. |
| `viewport-row-model-buffer-size` | `viewportRowModelBufferSize` | number | yes |  | When using viewport row model, sets the buffer size for the viewport. |
| `viewport-row-model-page-size` | `viewportRowModelPageSize` | number | yes |  | When using viewport row model, sets the page size for the viewport. |

## Column Definitions

| kebab | camelCase | type | init-only? | deprecated → replacement | description |
| --- | --- | --- | --- | --- | --- |
| `agg-func` | `aggFunc` | object |  |  | Name of function to use for aggregation. In-built options are: `sum`, `min`, `max`, `count`, `avg`, `first`, `last`. Also accepts a custom aggregation name or an aggregation function. |
| `allow-formula` | `allowFormula` | boolean |  |  | Allow formulas to be entered and evaluated in this column. |
| `allowed-agg-funcs` | `allowedAggFuncs` | array |  |  | Aggregation functions allowed on this column e.g. `['sum', 'avg']`. |
| `auto-header-height` | `autoHeaderHeight` | boolean |  |  | If enabled then the column header row will automatically adjust height to accommodate the size of the header cell. |
| `auto-height` | `autoHeight` | boolean |  |  | Set to `true` to have the grid calculate the height of a row based on contents of this column. |
| `calculated-expression` | `calculatedExpression` | string |  |  | Expression used to calculate this column's value from other columns in the same row. |
| `cell-aria-role` | `cellAriaRole` | string |  |  | Used for screen reader announcements - the role property of the cells that belong to this column. |
| `cell-class` | `cellClass` | object |  |  | Class to use for the cell. Can be string, array of strings, or function that returns a string or array of strings. |
| `cell-class-rules` | `cellClassRules` | object |  |  | Rules which can be applied to include certain CSS classes. |
| `cell-data-type` | `cellDataType` | object |  |  | The data type of the cell values for this column. |
| `cell-editor` | `cellEditor` | any |  |  | Provide your own cell editor component for this column's cells. |
| `cell-editor-params` | `cellEditorParams` | any |  |  | Params to be passed to the `cellEditor` component. |
| `cell-editor-popup` | `cellEditorPopup` | boolean |  |  | Set to `true`, to have the cell editor appear in a popup. |
| `cell-editor-popup-position` | `cellEditorPopupPosition` | object |  |  | Set the position for the popup cell editor. Possible values are |
| `cell-editor-selector` | `cellEditorSelector` | object |  |  | Callback to select which cell editor to be used for a given row within the same column. |
| `cell-renderer` | `cellRenderer` | any |  |  | Provide your own cell Renderer component for this column's cells. |
| `cell-renderer-params` | `cellRendererParams` | any |  |  | Params to be passed to the `cellRenderer` component. |
| `cell-renderer-selector` | `cellRendererSelector` | object |  |  | Callback to select which cell renderer to be used for a given row within the same column. |
| `cell-style` | `cellStyle` | object |  |  | An object of CSS values / or function returning an object of CSS values for a particular cell. |
| `chart-data-type` | `chartDataType` | object |  |  | Defines the chart data type that should be used for a column. |
| `checkbox-selection` | `checkboxSelection` | object |  | v32.2 Use the new selection API instead. See `GridOptions.rowSelection` Set to `true` (or return `true` from function) to render a selection checkbox in the column. |  |
| `col-id` | `colId` | string |  |  | The unique ID to give the column. This is optional. If missing, the ID will default to the field. |
| `col-span` | `colSpan` | object |  |  | By default, each cell will take up the width of one column. You can change this behaviour to allow cells to span multiple columns. |
| `column-chooser-params` | `columnChooserParams` | object |  |  | Params used to change the behaviour and appearance of the Column Chooser/Columns Menu tab. |
| `column-group-show` | `columnGroupShow` | object |  |  | Whether to only show the column when the group is open / closed. If not set the column is always displayed as part of the group. |
| `comparator` | `comparator` | object |  |  | Override the default sorting order by providing a custom sort comparator, or a map of comparators for different `SortType`s. |
| `context` | `context` | any |  |  | Context property that can be used to associate arbitrary application data with this column definition. |
| `context-menu-items` | `contextMenuItems` | object |  |  | Customise the list of menu items available in the context menu. |
| `date-component` | `dateComponent` | any |  |  | Custom date selection component to be used in Date Filters and Date Floating Filters for this column. |
| `date-component-params` | `dateComponentParams` | any |  |  | The parameters to be passed to the `dateComponent`. |
| `default-agg-func` | `defaultAggFunc` | string |  |  | The name of the aggregation function to use for this column when it is enabled via the GUI. |
| `dnd-source` | `dndSource` | object |  |  | `boolean` or `Function`. Set to `true` (or return `true` from function) to allow dragging for native drag and drop. |
| `dnd-source-on-row-drag` | `dndSourceOnRowDrag` | object |  |  | Function to allow custom drag functionality for native drag and drop. |
| `editable` | `editable` | object |  |  | Set to `true` if this column is editable, otherwise `false`. Can also be a function to have different rows editable. |
| `enable-cell-change-flash` | `enableCellChangeFlash` | boolean |  |  | Set to `true` to flash a cell when it's refreshed. |
| `enable-pivot` | `enablePivot` | boolean |  |  | Set to `true` if you want to be able to pivot by this column via the GUI. This will not block the API or properties being used to achieve pivot. |
| `enable-row-group` | `enableRowGroup` | boolean |  |  | Set to `true` if you want to be able to row group by this column via the GUI. |
| `enable-show-values-as` | `enableShowValuesAs` | boolean |  |  | Shows the "Show Values As" submenu in the column menu. |
| `enable-value` | `enableValue` | boolean |  |  | Set to `true` if you want to be able to aggregate by this column via the GUI. |
| `equals` | `equals` | object |  |  | Custom comparator for values, used by renderer to know if values have changed. Cells whose values have not changed don't get refreshed. |
| `field` | `field` | object |  |  | The field of the row object to get the cell's data from. |
| `filter` | `filter` | any |  |  | Filter to use for this column. |
| `filter-params` | `filterParams` | any |  |  | Params to be passed to the filter component specified in `filter`. |
| `filter-value-getter` | `filterValueGetter` | object |  |  | Function or expression. Gets the value for filtering purposes. |
| `flex` | `flex` | number |  |  | Equivalent to `flex-grow` in CSS. When `flex` is set on one or more |
| `floating-filter` | `floatingFilter` | boolean |  |  | Whether to display a floating filter for this column. |
| `floating-filter-component` | `floatingFilterComponent` | any |  |  | The custom component to be used for rendering the floating filter. |
| `floating-filter-component-params` | `floatingFilterComponentParams` | any |  |  | Params to be passed to `floatingFilterComponent`. |
| `get-find-text` | `getFindText` | object |  |  | When using Find with custom cell renderers, this allows providing a custom value to search within. |
| `get-quick-filter-text` | `getQuickFilterText` | object |  |  | A function to tell the grid what Quick Filter text to use for this column if you don't want to use the default (which is calling `toString` on the value). |
| `group-hierarchy` | `groupHierarchy` | array |  |  | Specify a grouping hierarchy for this column. This generates one or more virtual columns to group or pivot by when this column is grouped or pivoted. |
| `group-row-editable` | `groupRowEditable` | object |  |  | Works like `editable`, but is evaluated only for group rows. When provided, group rows use |
| `group-row-value-setter` | `groupRowValueSetter` | object |  |  | Controls how a group row value edit is distributed to descendant rows. |
| `header-checkbox-selection` | `headerCheckboxSelection` | object |  | v32.2 Use the new selection API instead. See `GridOptions.rowSelection` If `true` or the callback returns `true`, a 'select all' checkbox will be put into the header. |  |
| `header-checkbox-selection-current-page-only` | `headerCheckboxSelectionCurrentPageOnly` | boolean |  | v32.2 Use the new selection API instead. See `GridOptions.rowSelection` If `true`, the header checkbox selection will only select nodes on the current page. |  |
| `header-checkbox-selection-filtered-only` | `headerCheckboxSelectionFilteredOnly` | boolean |  | v32.2 Use the new selection API instead. See `GridOptions.rowSelection` If `true`, the header checkbox selection will only select filtered items. |  |
| `header-class` | `headerClass` | object |  |  | CSS class to use for the header cell. Can be a string, array of strings, or function. |
| `header-component` | `headerComponent` | any |  |  | The custom header component to be used for rendering the component header. If none specified the default AG Grid header component is used. |
| `header-component-params` | `headerComponentParams` | any |  |  | The parameters to be passed to the `headerComponent`. |
| `header-name` | `headerName` | string |  |  | The name to render in the column header. If not specified and field is specified, the field name will be used as the header name. |
| `header-style` | `headerStyle` | object |  |  | An object of CSS values / or function returning an object of CSS values for a particular header. |
| `header-tooltip` | `headerTooltip` | string |  |  | Tooltip for the column header, `headerTooltipValueGetter` takes precedence if set. |
| `header-tooltip-value-getter` | `headerTooltipValueGetter` | object |  |  | Callback that should return the string to use for a tooltip. |
| `header-value-getter` | `headerValueGetter` | object |  |  | Function or expression. Gets the value for display in the header. |
| `hide` | `hide` | boolean |  |  | Set to `true` for this column to be hidden. |
| `icons` | `icons` | object | yes |  | Icons to use inside the column instead of the grid's default icons. Leave undefined to use defaults. |
| `initial-agg-func` | `initialAggFunc` | object | yes |  | Same as `aggFunc`, except only applied when creating a new column. Not applied when updating column definitions. |
| `initial-flex` | `initialFlex` | number | yes |  | Same as `flex`, except only applied when creating a new column. Not applied when updating column definitions. |
| `initial-hide` | `initialHide` | boolean | yes |  | Same as `hide`, except only applied when creating a new column. Not applied when updating column definitions. |
| `initial-pinned` | `initialPinned` | object | yes |  | Same as `pinned`, except only applied when creating a new column. Not applied when updating column definitions. |
| `initial-pivot` | `initialPivot` | boolean | yes |  | Same as `pivot`, except only applied when creating a new column. Not applied when updating column definitions. |
| `initial-pivot-index` | `initialPivotIndex` | number | yes |  | Same as `pivotIndex`, except only applied when creating a new column. Not applied when updating column definitions. |
| `initial-row-group` | `initialRowGroup` | boolean | yes |  | Same as `rowGroup`, except only applied when creating a new column. Not applied when updating column definitions. |
| `initial-row-group-index` | `initialRowGroupIndex` | number | yes |  | Same as `rowGroupIndex`, except only applied when creating a new column. Not applied when updating column definitions. |
| `initial-show-values-as` | `initialShowValuesAs` | object | yes |  | Same as `showValuesAs`, except only applied when creating a new column. |
| `initial-sort` | `initialSort` | object | yes |  | Same as `sort`, except only applied when creating a new column. Not applied when updating column definitions. |
| `initial-sort-index` | `initialSortIndex` | number | yes |  | Same as `sortIndex`, except only applied when creating a new column. Not applied when updating column definitions. |
| `initial-value-index` | `initialValueIndex` | number | yes |  | Same as `valueIndex`, except only applied when creating a new column. Not applied when updating column definitions. |
| `initial-width` | `initialWidth` | number | yes |  | Same as `width`, except only applied when creating a new column. Not applied when updating column definitions. |
| `key-creator` | `keyCreator` | object |  |  | Function to return a string key for a value. |
| `loading-cell-renderer` | `loadingCellRenderer` | any |  |  | The renderer to be used while either |
| `loading-cell-renderer-params` | `loadingCellRendererParams` | any |  |  | Params to be passed to the `loadingCellRenderer` component. |
| `loading-cell-renderer-selector` | `loadingCellRendererSelector` | object |  |  | Callback to select which loading renderer to be used. |
| `lock-pinned` | `lockPinned` | boolean |  |  | Set to true to block the user pinning the column, the column can only be pinned via definitions or API. |
| `lock-position` | `lockPosition` | object |  |  | Lock a column to position to `'left'` or`'right'` to always have this column displayed in that position. `true` is treated as `'left'` |
| `lock-visible` | `lockVisible` | boolean |  |  | Set to `true` to block making column visible / hidden via the UI (API will still work). |
| `main-menu-items` | `mainMenuItems` | object |  |  | Customise the list of menu items available in the column menu. |
| `max-width` | `maxWidth` | number |  |  | Maximum width in pixels for the cell. |
| `menu-tabs` | `menuTabs` | array |  |  | Set to an array containing zero, one or many of the following options: `'filterMenuTab' \| 'generalMenuTab' \| 'columnsMenuTab'`. |
| `min-width` | `minWidth` | number |  |  | Minimum width in pixels for the cell. |
| `on-cell-clicked` | `onCellClicked` | function |  |  | Callback called when a cell is clicked. |
| `on-cell-context-menu` | `onCellContextMenu` | function |  |  | Callback called when a cell is right clicked. |
| `on-cell-double-clicked` | `onCellDoubleClicked` | function |  |  | Callback called when a cell is double clicked. |
| `on-cell-value-changed` | `onCellValueChanged` | function |  |  | Callback for after the value of a cell has changed, either due to editing or the application calling `api.setValue()`. |
| `pinned` | `pinned` | object |  |  | Pin a column to one side: `right` or `left`. A value of `true` is converted to `'left'`. |
| `pivot` | `pivot` | boolean |  |  | Set to true to pivot by this column. |
| `pivot-comparator` | `pivotComparator` | object | yes |  | Only for CSRM, see [SSRM Pivoting](https://www.ag-grid.com/javascript-data-grid/server-side-model-pivoting/). |
| `pivot-index` | `pivotIndex` | number |  |  | Set this in columns you want to pivot by. |
| `pivot-keys` | `pivotKeys` | array |  |  | Never set this, it is used internally by grid when doing in-grid pivoting |
| `pivot-total-column-ids` | `pivotTotalColumnIds` | array |  |  | Never set this, it is used internally by grid when doing in-grid pivoting |
| `pivot-value-column` | `pivotValueColumn` | object |  |  | Never set this, it is used internally by grid when doing in-grid pivoting |
| `ref-data` | `refData` | object |  |  | Provided a reference data map to be used to map column values to their respective value from the map. |
| `resizable` | `resizable` | boolean |  |  | Set to `false` to disable resizing which is enabled by default. |
| `row-drag` | `rowDrag` | object |  |  | `boolean` or `Function`. Set to `true` (or return `true` from function) to allow row dragging. |
| `row-drag-text` | `rowDragText` | object |  |  | A callback that should return a string to be displayed by the `rowDragComp` while dragging a row. |
| `row-group` | `rowGroup` | boolean |  |  | Set to `true` to row group by this column. |
| `row-group-index` | `rowGroupIndex` | number |  |  | Set this in columns you want to group by. |
| `row-grouping-hierarchy` | `rowGroupingHierarchy` | array |  | deprecated | Specify a grouping hierarchy for this column. This generates one or more virtual columns to group or pivot by when this column is grouped or pivoted. |
| `row-span` | `rowSpan` | object |  |  | By default, each cell will take up the height of one row. You can change this behaviour to allow cells to span multiple rows. |
| `show-disabled-checkboxes` | `showDisabledCheckboxes` | boolean |  | v32.2 Use the new selection API instead. See `GridOptions.rowSelection` Set to `true` to display a disabled checkbox when row is not selectable and checkboxes are enabled. |  |
| `show-row-group` | `showRowGroup` | object | yes |  | Set to true to have the grid place the values for the group into the cell, or put the name of a grouped column to just show that group. |
| `show-values-as` | `showValuesAs` | object |  |  | The active "Show Values As" mode for this column. |
| `show-values-as-def` | `showValuesAsDef` | object |  |  | Per-column "Show Values As" configuration: `precision`, `suppressHeaderIndicator`, and user-provided |
| `single-click-edit` | `singleClickEdit` | boolean |  |  | Set to `true` to have cells under this column enter edit mode after single click. |
| `sort` | `sort` | object |  |  | Set the default sort. |
| `sort-index` | `sortIndex` | number |  |  | If sorting more than one column by default, specifies order in which the sorting should be applied. |
| `sortable` | `sortable` | boolean |  |  | Set to `false` to disable sorting which is enabled by default. |
| `sorting-order` | `sortingOrder` | array |  |  | An array defining the order in which sorting occurs (if sorting is enabled). |
| `span-rows` | `spanRows` | object |  |  | Set to `true` to automatically merge cells in this column with equal values. Provide a callback to specify custom merging logic. |
| `suppress-auto-size` | `suppressAutoSize` | boolean |  |  | Set to `true` if you do not want this column to be auto-resizable during 'size to contents' operations. |
| `suppress-columns-tool-panel` | `suppressColumnsToolPanel` | boolean |  |  | Set to `true` if you do not want this column or group to appear in the Columns Tool Panel. |
| `suppress-fill-handle` | `suppressFillHandle` | boolean |  |  | Set to true to prevent the fillHandle from being rendered in any cell that belongs to this column |
| `suppress-filters-tool-panel` | `suppressFiltersToolPanel` | boolean |  |  | Set to `true` if you do not want this column (filter) or group (filter group) to appear in the Filters Tool Panel. |
| `suppress-floating-filter-button` | `suppressFloatingFilterButton` | boolean |  |  | If `true`, the button in the floating filter that opens the parent filter in a popup will not be displayed. |
| `suppress-header-context-menu` | `suppressHeaderContextMenu` | boolean |  |  | Set to `true` to not display the column menu when the column header is right-clicked. |
| `suppress-header-filter-button` | `suppressHeaderFilterButton` | boolean |  |  | Set to `true` to not display the filter button in the column header. |
| `suppress-header-keyboard-event` | `suppressHeaderKeyboardEvent` | object |  |  | Suppress the grid taking action for the relevant keyboard event when a header is focused. |
| `suppress-header-menu-button` | `suppressHeaderMenuButton` | boolean |  |  | Set to `true` if no menu button should be shown for this column header. |
| `suppress-keyboard-event` | `suppressKeyboardEvent` | object |  |  | Allows the user to suppress certain keyboard events in the grid cell. |
| `suppress-movable` | `suppressMovable` | boolean |  |  | Set to `true` if you do not want this column to be movable via dragging. |
| `suppress-navigable` | `suppressNavigable` | object |  |  | Set to `true` if this column is not navigable (i.e. cannot be tabbed into), otherwise `false`. |
| `suppress-note-actions` | `suppressNoteActions` | object |  |  | Set to `true` to suppress built-in note actions for this column. |
| `suppress-paste` | `suppressPaste` | object |  |  | Pasting is on by default as long as cells are editable (non-editable cells cannot be modified, even with a paste operation). |
| `suppress-size-to-fit` | `suppressSizeToFit` | boolean |  |  | Set to `true` if you want this column's width to be fixed during 'size to fit' operations. |
| `suppress-span-header-height` | `suppressSpanHeaderHeight` | boolean |  |  | Set to `true` if you don't want the column header for this column to span the whole height of the header container. |
| `tool-panel-class` | `toolPanelClass` | object |  |  | CSS class to use for the tool panel cell. Can be a string, array of strings, or function. |
| `tooltip-component` | `tooltipComponent` | any |  |  | Provide your own tooltip component for the column. |
| `tooltip-component-params` | `tooltipComponentParams` | any |  |  | The params used to configure `tooltipComponent`. |
| `tooltip-component-selector` | `tooltipComponentSelector` | object |  |  | Callback to select which tooltip component to be used for a given row within the same column. |
| `tooltip-field` | `tooltipField` | object |  |  | The field of the tooltip to apply to the cell. |
| `tooltip-value-getter` | `tooltipValueGetter` | object |  |  | Callback that should return the string to use for a tooltip, `tooltipField` takes precedence if set. |
| `type` | `type` | array |  |  | A comma separated string or array of strings containing `ColumnType` keys which can be used as a template for a column. |
| `un-sort-icon` | `unSortIcon` | boolean |  |  | Set to `true` if you want the unsorted icon to be shown when no sort is applied to this column. |
| `use-value-formatter-for-export` | `useValueFormatterForExport` | boolean |  |  | By default, values are formatted using the column's `valueFormatter` when exporting data from the grid. |
| `use-value-parser-for-import` | `useValueParserForImport` | boolean |  |  | By default, values are parsed using the column's `valueParser` when importing data to the grid. |
| `value-formatter` | `valueFormatter` | object |  |  | A function or expression to format a value, should return a string. |
| `value-getter` | `valueGetter` | object |  |  | Function or expression. Gets the value from your data for display. |
| `value-index` | `valueIndex` | number |  |  | The position of this column in the order of value columns when aggregating in pivot mode. |
| `value-parser` | `valueParser` | object |  |  | Function or expression. Parses the value for saving. |
| `value-setter` | `valueSetter` | object |  |  | Function or expression. Sets the value into your data for saving. Return `true` if the data changed. |
| `width` | `width` | number |  |  | Initial width in pixels for the cell. |
| `wrap-header-text` | `wrapHeaderText` | boolean |  |  | If enabled then column header names that are too long for the column width will wrap onto the next line. Default `false` |
| `wrap-text` | `wrapText` | boolean |  |  | Set to `true` to have the text wrap inside the cell - typically used with `autoHeight`. |

## Column Groups

| kebab | camelCase | type | init-only? | deprecated → replacement | description |
| --- | --- | --- | --- | --- | --- |
| `auto-header-height` | `autoHeaderHeight` | boolean |  |  | If enabled then the column header row will automatically adjust height to accommodate the size of the header cell. |
| `cell-aria-role` | `cellAriaRole` | string |  |  | Used for screen reader announcements - the role property of the cells that belong to this column. |
| `children` | `children` | array |  |  | A list containing a mix of columns and column groups. |
| `column-group-show` | `columnGroupShow` | object |  |  | Whether to only show the column when the group is open / closed. If not set the column is always displayed as part of the group. |
| `context` | `context` | any |  |  | Context property that can be used to associate arbitrary application data with this column definition. |
| `group-id` | `groupId` | string |  |  | The unique ID to give the column. This is optional. If missing, a unique ID will be generated. This ID is used to identify the column group in the API. |
| `header-class` | `headerClass` | object |  |  | CSS class to use for the header cell. Can be a string, array of strings, or function. |
| `header-group-component` | `headerGroupComponent` | any |  |  | The custom header group component to be used for rendering the component header. If none specified the default AG Grid is used. |
| `header-group-component-params` | `headerGroupComponentParams` | any |  |  | The params used to configure the `headerGroupComponent`. |
| `header-name` | `headerName` | string |  |  | The name to render in the column header. If not specified and field is specified, the field name will be used as the header name. |
| `header-style` | `headerStyle` | object |  |  | An object of CSS values / or function returning an object of CSS values for a particular header. |
| `header-tooltip` | `headerTooltip` | string |  |  | Tooltip for the column header, `headerTooltipValueGetter` takes precedence if set. |
| `header-tooltip-value-getter` | `headerTooltipValueGetter` | object |  |  | Callback that should return the string to use for a tooltip. |
| `header-value-getter` | `headerValueGetter` | object |  |  | Function or expression. Gets the value for display in the header. |
| `main-menu-items` | `mainMenuItems` | object |  |  | Customise the list of menu items available in the column group header context menu (on right-click). |
| `marry-children` | `marryChildren` | boolean |  |  | Set to `true` to keep columns in this group beside each other in the grid. Moving the columns outside of the group (and hence breaking the group) is not allowed. |
| `open-by-default` | `openByDefault` | boolean |  |  | Set to `true` if this group should be opened by default. |
| `pivot-keys` | `pivotKeys` | array |  |  | Never set this, it is used internally by grid when doing in-grid pivoting |
| `suppress-columns-tool-panel` | `suppressColumnsToolPanel` | boolean |  |  | Set to `true` if you do not want this column or group to appear in the Columns Tool Panel. |
| `suppress-filters-tool-panel` | `suppressFiltersToolPanel` | boolean |  |  | Set to `true` if you do not want this column (filter) or group (filter group) to appear in the Filters Tool Panel. |
| `suppress-header-context-menu` | `suppressHeaderContextMenu` | boolean |  |  | Set to `true` to not display the column menu when the column header is right-clicked. |
| `suppress-header-keyboard-event` | `suppressHeaderKeyboardEvent` | object |  |  | Suppress the grid taking action for the relevant keyboard event when a header is focused. |
| `suppress-sticky-label` | `suppressStickyLabel` | boolean |  |  | If `true` the label of the Column Group will not scroll alongside the grid to always remain visible. |
| `tool-panel-class` | `toolPanelClass` | object |  |  | CSS class to use for the tool panel cell. Can be a string, array of strings, or function. |
| `tooltip-component` | `tooltipComponent` | any |  |  | Provide your own tooltip component for the column. |
| `tooltip-component-params` | `tooltipComponentParams` | any |  |  | The params used to configure `tooltipComponent`. |
| `wrap-header-text` | `wrapHeaderText` | boolean |  |  | If enabled then column header names that are too long for the column width will wrap onto the next line. Default `false` |

## Events

| kebab | event name | handler prop |
| --- | --- | --- |
| `advanced-filter-builder-visible-changed` | `advancedFilterBuilderVisibleChanged` | `onAdvancedFilterBuilderVisibleChanged` |
| `async-transactions-flushed` | `asyncTransactionsFlushed` | `onAsyncTransactionsFlushed` |
| `batch-editing-started` | `batchEditingStarted` | `onBatchEditingStarted` |
| `batch-editing-stopped` | `batchEditingStopped` | `onBatchEditingStopped` |
| `body-scroll` | `bodyScroll` | `onBodyScroll` |
| `body-scroll-end` | `bodyScrollEnd` | `onBodyScrollEnd` |
| `bulk-editing-started` | `bulkEditingStarted` | `onBulkEditingStarted` |
| `bulk-editing-stopped` | `bulkEditingStopped` | `onBulkEditingStopped` |
| `calculated-column-created` | `calculatedColumnCreated` | `onCalculatedColumnCreated` |
| `calculated-column-expression-changed` | `calculatedColumnExpressionChanged` | `onCalculatedColumnExpressionChanged` |
| `calculated-column-removed` | `calculatedColumnRemoved` | `onCalculatedColumnRemoved` |
| `calculated-column-validation-state-changed` | `calculatedColumnValidationStateChanged` | `onCalculatedColumnValidationStateChanged` |
| `cell-clicked` | `cellClicked` | `onCellClicked` |
| `cell-context-menu` | `cellContextMenu` | `onCellContextMenu` |
| `cell-double-clicked` | `cellDoubleClicked` | `onCellDoubleClicked` |
| `cell-edit-request` | `cellEditRequest` | `onCellEditRequest` |
| `cell-editing-started` | `cellEditingStarted` | `onCellEditingStarted` |
| `cell-editing-stopped` | `cellEditingStopped` | `onCellEditingStopped` |
| `cell-focused` | `cellFocused` | `onCellFocused` |
| `cell-key-down` | `cellKeyDown` | `onCellKeyDown` |
| `cell-mouse-down` | `cellMouseDown` | `onCellMouseDown` |
| `cell-mouse-out` | `cellMouseOut` | `onCellMouseOut` |
| `cell-mouse-over` | `cellMouseOver` | `onCellMouseOver` |
| `cell-selection-changed` | `cellSelectionChanged` | `onCellSelectionChanged` |
| `cell-selection-delete-end` | `cellSelectionDeleteEnd` | `onCellSelectionDeleteEnd` |
| `cell-selection-delete-start` | `cellSelectionDeleteStart` | `onCellSelectionDeleteStart` |
| `cell-value-changed` | `cellValueChanged` | `onCellValueChanged` |
| `chart-created` | `chartCreated` | `onChartCreated` |
| `chart-destroyed` | `chartDestroyed` | `onChartDestroyed` |
| `chart-options-changed` | `chartOptionsChanged` | `onChartOptionsChanged` |
| `chart-range-selection-changed` | `chartRangeSelectionChanged` | `onChartRangeSelectionChanged` |
| `column-everything-changed` | `columnEverythingChanged` | `onColumnEverythingChanged` |
| `column-group-opened` | `columnGroupOpened` | `onColumnGroupOpened` |
| `column-header-clicked` | `columnHeaderClicked` | `onColumnHeaderClicked` |
| `column-header-context-menu` | `columnHeaderContextMenu` | `onColumnHeaderContextMenu` |
| `column-header-mouse-leave` | `columnHeaderMouseLeave` | `onColumnHeaderMouseLeave` |
| `column-header-mouse-over` | `columnHeaderMouseOver` | `onColumnHeaderMouseOver` |
| `column-menu-visible-changed` | `columnMenuVisibleChanged` | `onColumnMenuVisibleChanged` |
| `column-moved` | `columnMoved` | `onColumnMoved` |
| `column-pinned` | `columnPinned` | `onColumnPinned` |
| `column-pivot-changed` | `columnPivotChanged` | `onColumnPivotChanged` |
| `column-pivot-mode-changed` | `columnPivotModeChanged` | `onColumnPivotModeChanged` |
| `column-resized` | `columnResized` | `onColumnResized` |
| `column-row-group-changed` | `columnRowGroupChanged` | `onColumnRowGroupChanged` |
| `column-value-changed` | `columnValueChanged` | `onColumnValueChanged` |
| `column-visible` | `columnVisible` | `onColumnVisible` |
| `columns-reset` | `columnsReset` | `onColumnsReset` |
| `component-state-changed` | `componentStateChanged` | `onComponentStateChanged` |
| `context-menu-visible-changed` | `contextMenuVisibleChanged` | `onContextMenuVisibleChanged` |
| `cut-end` | `cutEnd` | `onCutEnd` |
| `cut-start` | `cutStart` | `onCutStart` |
| `displayed-columns-changed` | `displayedColumnsChanged` | `onDisplayedColumnsChanged` |
| `drag-cancelled` | `dragCancelled` | `onDragCancelled` |
| `drag-started` | `dragStarted` | `onDragStarted` |
| `drag-stopped` | `dragStopped` | `onDragStopped` |
| `expand-or-collapse-all` | `expandOrCollapseAll` | `onExpandOrCollapseAll` |
| `fill-end` | `fillEnd` | `onFillEnd` |
| `fill-start` | `fillStart` | `onFillStart` |
| `filter-changed` | `filterChanged` | `onFilterChanged` |
| `filter-modified` | `filterModified` | `onFilterModified` |
| `filter-opened` | `filterOpened` | `onFilterOpened` |
| `filter-ui-changed` | `filterUiChanged` | `onFilterUiChanged` |
| `find-changed` | `findChanged` | `onFindChanged` |
| `first-data-rendered` | `firstDataRendered` | `onFirstDataRendered` |
| `floating-filter-ui-changed` | `floatingFilterUiChanged` | `onFloatingFilterUiChanged` |
| `grid-columns-changed` | `gridColumnsChanged` | `onGridColumnsChanged` |
| `grid-pre-destroyed` | `gridPreDestroyed` | `onGridPreDestroyed` |
| `grid-ready` | `gridReady` | `onGridReady` |
| `grid-size-changed` | `gridSizeChanged` | `onGridSizeChanged` |
| `header-focused` | `headerFocused` | `onHeaderFocused` |
| `model-updated` | `modelUpdated` | `onModelUpdated` |
| `new-columns-loaded` | `newColumnsLoaded` | `onNewColumnsLoaded` |
| `pagination-changed` | `paginationChanged` | `onPaginationChanged` |
| `paste-end` | `pasteEnd` | `onPasteEnd` |
| `paste-start` | `pasteStart` | `onPasteStart` |
| `pinned-row-data-changed` | `pinnedRowDataChanged` | `onPinnedRowDataChanged` |
| `pinned-rows-changed` | `pinnedRowsChanged` | `onPinnedRowsChanged` |
| `pivot-max-columns-exceeded` | `pivotMaxColumnsExceeded` | `onPivotMaxColumnsExceeded` |
| `range-delete-end` | `rangeDeleteEnd` | `onRangeDeleteEnd` |
| `range-delete-start` | `rangeDeleteStart` | `onRangeDeleteStart` |
| `range-selection-changed` | `rangeSelectionChanged` | `onRangeSelectionChanged` |
| `redo-ended` | `redoEnded` | `onRedoEnded` |
| `redo-started` | `redoStarted` | `onRedoStarted` |
| `row-clicked` | `rowClicked` | `onRowClicked` |
| `row-data-updated` | `rowDataUpdated` | `onRowDataUpdated` |
| `row-double-clicked` | `rowDoubleClicked` | `onRowDoubleClicked` |
| `row-drag-cancel` | `rowDragCancel` | `onRowDragCancel` |
| `row-drag-end` | `rowDragEnd` | `onRowDragEnd` |
| `row-drag-enter` | `rowDragEnter` | `onRowDragEnter` |
| `row-drag-leave` | `rowDragLeave` | `onRowDragLeave` |
| `row-drag-move` | `rowDragMove` | `onRowDragMove` |
| `row-editing-started` | `rowEditingStarted` | `onRowEditingStarted` |
| `row-editing-stopped` | `rowEditingStopped` | `onRowEditingStopped` |
| `row-group-opened` | `rowGroupOpened` | `onRowGroupOpened` |
| `row-resize-ended` | `rowResizeEnded` | `onRowResizeEnded` |
| `row-resize-started` | `rowResizeStarted` | `onRowResizeStarted` |
| `row-selected` | `rowSelected` | `onRowSelected` |
| `row-value-changed` | `rowValueChanged` | `onRowValueChanged` |
| `selection-changed` | `selectionChanged` | `onSelectionChanged` |
| `sort-changed` | `sortChanged` | `onSortChanged` |
| `state-updated` | `stateUpdated` | `onStateUpdated` |
| `store-refreshed` | `storeRefreshed` | `onStoreRefreshed` |
| `tool-panel-size-changed` | `toolPanelSizeChanged` | `onToolPanelSizeChanged` |
| `tool-panel-visible-changed` | `toolPanelVisibleChanged` | `onToolPanelVisibleChanged` |
| `tooltip-hide` | `tooltipHide` | `onTooltipHide` |
| `tooltip-show` | `tooltipShow` | `onTooltipShow` |
| `undo-ended` | `undoEnded` | `onUndoEnded` |
| `undo-started` | `undoStarted` | `onUndoStarted` |
| `viewport-changed` | `viewportChanged` | `onViewportChanged` |
| `virtual-columns-changed` | `virtualColumnsChanged` | `onVirtualColumnsChanged` |
| `virtual-row-removed` | `virtualRowRemoved` | `onVirtualRowRemoved` |
