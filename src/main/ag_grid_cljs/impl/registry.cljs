(ns ag-grid-cljs.impl.registry
  "GENERATED — do not edit by hand. Regenerate with tools/codegen on an AG Grid
  dependency bump (see tools/codegen/extract.mjs, ADR 0007).

  The dev-only catalog of AG Grid option/event keys powering typo warnings and
  the kebab<->camel reference table. The runtime conversion (ADR 0005) never
  consults it — camelization is a deterministic algorithm. Only :initial? is
  load-bearing beyond dev warnings: it is the options differ's sole
  updatable-vs-initial-only classifier (ADR 0008).

  The registry value is guarded by ^boolean goog.DEBUG so :advanced compilation
  with {goog.DEBUG false} dead-code-eliminates the whole literal from production
  builds. Reference it ONLY from goog.DEBUG-guarded code to keep that true.")

(def registry
  "Rich key registry, pinned to a single AG Grid version. Blocks:
  :grid-options / :col-def / :col-group-def / :events, plus :ag-grid-version.
  Per-key shape {:camel :type :default :initial? :deprecated :doc}; :default,
  :initial?, :deprecated and :doc are present only when AG Grid declares them.
  nil in production builds (goog.DEBUG false → DCE)."
  (when ^boolean goog.DEBUG
    {:ag-grid-version "36.0.2"
     :grid-options
     {:accented-sort {:camel "accentedSort" :type :boolean :default "false" :doc "Set to `true` to specify that the sort should take accented characters into account. If this feature is turned on the sort will be slower."}
     :active-overlay {:camel "activeOverlay" :type :any :doc "Display an overlay on demand. If provided takes precedence over the grid provided overlays."}
     :active-overlay-params {:camel "activeOverlayParams" :type :any :doc "Custom parameters to be supplied to the `activeOverlay` component in addition to `IOverlayParams`. Updating the params will trigger a refresh of the active overlay."}
     :advanced-filter-builder-params {:camel "advancedFilterBuilderParams" :type :object :doc "Customise the parameters passed to the Advanced Filter Builder."}
     :advanced-filter-params {:camel "advancedFilterParams" :type :object :doc "Customise the parameters passed to the Advanced Filter"}
     :advanced-filter-parent {:camel "advancedFilterParent" :type :object :doc "DOM element to use as the parent for the Advanced Filter to allow it to appear outside of the grid."}
     :agg-funcs {:camel "aggFuncs" :type :object :initial? true :doc "A map of 'function name' to 'function' for custom aggregation functions."}
     :aggregate-only-changed-columns {:camel "aggregateOnlyChangedColumns" :type :boolean :default "false" :doc "When using change detection, only the updated column will be re-aggregated."}
     :aligned-grids {:camel "alignedGrids" :type :function :doc "A list of grids to treat as Aligned Grids."}
     :allow-context-menu-with-control-key {:camel "allowContextMenuWithControlKey" :type :boolean :default "false" :doc "Allows context menu to show, even when `Ctrl` key is held down."}
     :allow-drag-from-columns-tool-panel {:camel "allowDragFromColumnsToolPanel" :type :boolean :default "false" :doc "Allow reordering and pinning columns by dragging columns from the Columns Tool Panel to the grid."}
     :allow-show-change-after-filter {:camel "allowShowChangeAfterFilter" :type :boolean :default "false" :initial? true :doc "Set to `true` to have cells flash after data changes even when the change is due to filtering."}
     :always-aggregate-at-root-level {:camel "alwaysAggregateAtRootLevel" :type :boolean :default "false" :doc "When using aggregations, the grid will always calculate the root level aggregation value."}
     :always-multi-sort {:camel "alwaysMultiSort" :type :boolean :default "false" :doc "Set to `true` to always multi-sort when the user clicks a column header, regardless of key presses."}
     :always-pass-filter {:camel "alwaysPassFilter" :type :object :doc "Allows rows to always be displayed, even if they don't match the applied filtering."}
     :always-show-horizontal-scroll {:camel "alwaysShowHorizontalScroll" :type :boolean :default "false" :doc "Set to `true` to always show the horizontal scrollbar."}
     :always-show-vertical-scroll {:camel "alwaysShowVerticalScroll" :type :boolean :default "false" :doc "Set to `true` to always show the vertical scrollbar."}
     :animate-column-resizing {:camel "animateColumnResizing" :type :boolean :default "false" :doc "Set to `true` to animate changes to column width when auto-sizing the columns."}
     :animate-rows {:camel "animateRows" :type :boolean :default "true" :doc "Set to `false` to disable Row Animation which is enabled by default."}
     :apply-quick-filter-before-pivot-or-agg {:camel "applyQuickFilterBeforePivotOrAgg" :type :boolean :default "false" :doc "When pivoting, Quick Filter is only applied on the pivoted data"}
     :async-transaction-wait-millis {:camel "asyncTransactionWaitMillis" :type :number :doc "How many milliseconds to wait before executing a batch of async transactions."}
     :auto-generate-column-defs {:camel "autoGenerateColumnDefs" :type :object :default "false" :doc "When enabled, column definitions are generated automatically from the first row of `rowData` whenever row data is set or updated."}
     :auto-group-column-def {:camel "autoGroupColumnDef" :type :object :doc "Allows specifying the group 'auto column' if you are not happy with the default. If grouping, this column definition is included as the first column in the grid. If not grouping, this column is not included."}
     :auto-size-padding {:camel "autoSizePadding" :type :number :default "20" :doc "Number of pixels to add to a column width after the [auto-sizing](./column-sizing/#auto-size-columns-to-fit-cell-contents) calculation."}
     :auto-size-strategy {:camel "autoSizeStrategy" :type :object :initial? true :doc "Auto-size the columns when the grid is loaded. Can size to fit the grid width, fit a provided width, or fit the cell contents."}
     :block-load-debounce-millis {:camel "blockLoadDebounceMillis" :type :number :initial? true :doc "How many milliseconds to wait before loading a block. Useful when scrolling over many blocks, as it prevents blocks loading until scrolling has settled."}
     :cache-block-size {:camel "cacheBlockSize" :type :number :default "100" :doc "How many rows for each block in the store, i.e. how many rows returned from the server at a time."}
     :cache-overflow-size {:camel "cacheOverflowSize" :type :number :default "1" :initial? true :doc "How many extra blank rows to display to the user at the end of the dataset, which sets the vertical scroll and then allows the grid to request viewing more rows of data."}
     :cache-quick-filter {:camel "cacheQuickFilter" :type :boolean :default "false" :initial? true :doc "Set to `true` to turn on the Quick Filter cache, used to improve performance when using the Quick Filter."}
     :calculated-columns {:camel "calculatedColumns" :type :object :doc "Enables and configures Calculated Columns."}
     :cell-fade-duration {:camel "cellFadeDuration" :type :number :default "1000" :doc "Sets the duration in milliseconds of how long the \"flashed\" state animation takes to fade away after the timer set by `cellFlashDuration` has completed."}
     :cell-flash-duration {:camel "cellFlashDuration" :type :number :default "500" :doc "Sets the duration in milliseconds of how long a cell should remain in its \"flashed\" state."}
     :cell-selection {:camel "cellSelection" :type :object :doc "Configure cell selection."}
     :chart-menu-items {:camel "chartMenuItems" :type :object :doc "Get chart menu items. Only applies when using AG Charts Enterprise."}
     :chart-theme-overrides {:camel "chartThemeOverrides" :type :object :initial? true :doc "Chart theme overrides applied to all themes."}
     :chart-themes {:camel "chartThemes" :type :array :default "['ag-default', 'ag-material', 'ag-sheets', 'ag-polychroma', 'ag-vivid'];" :initial? true :doc "The list of chart themes that a user can choose from in the chart panel."}
     :chart-tool-panels-def {:camel "chartToolPanelsDef" :type :object :initial? true :doc "Allows customisation of the Chart Tool Panels, such as changing the tool panels visibility and order, as well as choosing which charts should be displayed in the chart panel."}
     :clipboard-delimiter {:camel "clipboardDelimiter" :type :string :default "'\\t'" :doc "Specify the delimiter to use when copying to clipboard."}
     :col-resize-default {:camel "colResizeDefault" :type :object :doc "Set to `'shift'` to have shift-resize as the default resize operation (same as user holding down `Shift` while resizing)."}
     :column-defs {:camel "columnDefs" :type :array :doc "Array of Column / Column Group definitions."}
     :column-hover-highlight {:camel "columnHoverHighlight" :type :boolean :default "false" :doc "Set to `true` to highlight columns by adding the `ag-column-hover` CSS class."}
     :column-menu {:camel "columnMenu" :type :object :default "'new'" :initial? true :doc "Changes the display type of the column menu."}
     :column-types {:camel "columnTypes" :type :object :doc "An object map of custom column types which contain groups of properties that column definitions can reuse by referencing in their `type` property."}
     :components {:camel "components" :type :object :initial? true :doc "A map of component names to components."}
     :context {:camel "context" :type :any :initial? true :doc "Provides a context object that is provided to different callbacks the grid uses. Used for passing additional information to the callbacks used by your application."}
     :copy-group-headers-to-clipboard {:camel "copyGroupHeadersToClipboard" :type :boolean :default "false" :doc "Set to `true` to also include group headers when copying to clipboard using `Ctrl + C` clipboard."}
     :copy-headers-to-clipboard {:camel "copyHeadersToClipboard" :type :boolean :default "false" :doc "Set to `true` to also include headers when copying to clipboard using `Ctrl + C` clipboard."}
     :create-chart-container {:camel "createChartContainer" :type :object :initial? true :doc "Callback to enable displaying the chart in an alternative chart container."}
     :custom-chart-themes {:camel "customChartThemes" :type :object :initial? true :doc "A map containing custom chart themes."}
     :data-type-definitions {:camel "dataTypeDefinitions" :type :object :doc "An object map of cell data types to their definitions."}
     :datasource {:camel "datasource" :type :object :doc "Provide the datasource for infinite scrolling."}
     :debounce-vertical-scrollbar {:camel "debounceVerticalScrollbar" :type :boolean :default "false" :initial? true :doc "Set to `true` to debounce the vertical scrollbar. Can provide smoother scrolling on slow machines."}
     :debug {:camel "debug" :type :boolean :default "false" :initial? true :doc "Set this to `true` to enable debug information from the grid and related components. Will result in additional logging being output, but very useful when investigating problems."}
     :default-col-def {:camel "defaultColDef" :type :object :doc "A default column definition. Items defined in the actual column definitions get precedence."}
     :default-col-group-def {:camel "defaultColGroupDef" :type :object :initial? true :doc "A default column group definition. All column group definitions will use these properties. Items defined in the actual column group definition get precedence."}
     :default-csv-export-params {:camel "defaultCsvExportParams" :type :object :doc "A default configuration object used to export to CSV."}
     :default-excel-export-params {:camel "defaultExcelExportParams" :type :object :doc "A default configuration object used to export to Excel."}
     :delta-sort {:camel "deltaSort" :type :boolean :default "false" :doc "When enabled, sorts only the rows added/updated by a transaction."}
     :detail-cell-renderer {:camel "detailCellRenderer" :type :any :doc "Provide a custom `detailCellRenderer` to use when a master row is expanded."}
     :detail-cell-renderer-params {:camel "detailCellRendererParams" :type :any :doc "Specifies the params to be used by the Detail Cell Renderer. Can also be a function that provides the params to enable dynamic definitions of the params."}
     :detail-row-auto-height {:camel "detailRowAutoHeight" :type :boolean :initial? true :doc "Set to `true` to have the detail grid dynamically change it's height to fit it's rows."}
     :detail-row-height {:camel "detailRowHeight" :type :number :initial? true :doc "Set fixed height in pixels for each detail row."}
     :does-external-filter-pass {:camel "doesExternalFilterPass" :type :object :doc "Should return `true` if external filter passes, otherwise `false`."}
     :dom-layout {:camel "domLayout" :type :object :default "'normal'" :doc "Switch between layout options: `normal`, `autoHeight`, `print`."}
     :drag-and-drop-image-component {:camel "dragAndDropImageComponent" :type :any :initial? true :doc "Provide a custom drag and drop image component."}
     :drag-and-drop-image-component-params {:camel "dragAndDropImageComponentParams" :type :any :doc "Customise the parameters provided to the Drag and Drop Image Component."}
     :edit-type {:camel "editType" :type :object :doc "Set to `'fullRow'` to enable Full Row Editing. Otherwise leave blank to edit one cell at a time."}
     :embed-full-width-rows {:camel "embedFullWidthRows" :type :boolean :doc "Set to `true` to have the Full Width Rows embedded in grid's main container so they can be scrolled horizontally."}
     :enable-advanced-filter {:camel "enableAdvancedFilter" :type :boolean :default "false" :doc "Set to true to enable the Advanced Filter."}
     :enable-browser-tooltips {:camel "enableBrowserTooltips" :type :boolean :default "false" :initial? true :doc "Set to `true` to use the browser's default tooltip instead of using the grid's Tooltip Component."}
     :enable-cell-editing-on-backspace {:camel "enableCellEditingOnBackspace" :type :boolean :doc "Forces Cell Editing to start when backspace is pressed. This is only relevant for MacOS users."}
     :enable-cell-expressions {:camel "enableCellExpressions" :type :boolean :default "false" :initial? true :doc "Set to `true` to allow cell expressions."}
     :enable-cell-span {:camel "enableCellSpan" :type :boolean :default "false" :initial? true :doc "When `true`, enables the cell span feature allowing for the use of the `colDef.spanRows` property."}
     :enable-cell-text-selection {:camel "enableCellTextSelection" :type :boolean :default "false" :doc "Set to `true` to be able to select the text within cells."}
     :enable-charts {:camel "enableCharts" :type :boolean :default "false" :doc "Set to `true` to Enable Charts."}
     :enable-fill-handle {:camel "enableFillHandle" :type :boolean :default "false" :deprecated "v32.2 Use `cellSelection.handle` instead" :doc "Set to `true` to enable the Fill Handle."}
     :enable-filter-handlers {:camel "enableFilterHandlers" :type :boolean :initial? true :doc "Enable filter handlers for custom filter components."}
     :enable-group-edit {:camel "enableGroupEdit" :type :boolean :initial? true}
     :enable-range-handle {:camel "enableRangeHandle" :type :boolean :default "false" :deprecated "v32.2 Use `cellSelection.handle` instead" :doc "Set to `true` to enable the Range Handle."}
     :enable-range-selection {:camel "enableRangeSelection" :type :boolean :default "false" :deprecated "v32.2 Use `cellSelection = true` instead" :doc "Set to `true` to enable Range Selection."}
     :enable-row-pinning {:camel "enableRowPinning" :type :object :doc "Determines whether manual row pinning is enabled via the row context menu."}
     :enable-rtl {:camel "enableRtl" :type :boolean :default "false" :initial? true :doc "Set to `true` to operate the grid in RTL (Right to Left) mode."}
     :enable-strict-pivot-column-order {:camel "enableStrictPivotColumnOrder" :type :boolean :default "false" :doc "Resets pivot column order when impacted by filters, data or configuration changes"}
     :ensure-dom-order {:camel "ensureDomOrder" :type :boolean :default "false" :initial? true :doc "When `true`, the order of rows and columns in the DOM are consistent with what is on screen."}
     :enter-navigates-vertically {:camel "enterNavigatesVertically" :type :boolean :default "false" :doc "Set to `true` along with `enterNavigatesVerticallyAfterEdit` to have Excel-style behaviour for the `Enter` key."}
     :enter-navigates-vertically-after-edit {:camel "enterNavigatesVerticallyAfterEdit" :type :boolean :default "false" :doc "Set to `true` along with `enterNavigatesVertically` to have Excel-style behaviour for the 'Enter' key."}
     :excel-styles {:camel "excelStyles" :type :array :initial? true :doc "A list (array) of Excel styles to be used when exporting to Excel with styles."}
     :exclude-children-when-tree-data-filtering {:camel "excludeChildrenWhenTreeDataFiltering" :type :boolean :default "false" :doc "Set to `true` to override the default tree data filtering behaviour to instead exclude child nodes from filter results."}
     :fill-handle-direction {:camel "fillHandleDirection" :type :object :default "'xy'" :deprecated "v32.2 Use `cellSelection.handle.direction` instead" :doc "Set to `'x'` to force the fill handle direction to horizontal, or set to `'y'` to force the fill handle direction to vertical."}
     :fill-operation {:camel "fillOperation" :type :object :deprecated "v32.2 Use `cellSelection.handle.setFillValue` instead" :doc "Callback to fill values instead of simply copying values or increasing number values using linear progression."}
     :filter-handlers {:camel "filterHandlers" :type :object :initial? true :doc "A map of filter handler key to filter handler function."}
     :find-options {:camel "findOptions" :type :object :doc "Options for the Find feature."}
     :find-search-value {:camel "findSearchValue" :type :string :doc "Text to find within the grid."}
     :floating-filters-height {:camel "floatingFiltersHeight" :type :number :doc "The height in pixels for the row containing the floating filters. If not specified, it uses the theme value of `header-height`."}
     :focus-grid-inner-element {:camel "focusGridInnerElement" :type :object :doc "Allows overriding the element that will be focused when the grid receives focus from outside elements (tabbing into the grid)."}
     :formula-data-source {:camel "formulaDataSource" :type :object :initial? true :doc "Provide a data source to control where formulas are stored and retrieved."}
     :formula-funcs {:camel "formulaFuncs" :type :object :initial? true :doc "A map of 'function name' to 'function' for custom functions that are used for formulas."}
     :full-width-cell-renderer {:camel "fullWidthCellRenderer" :type :any :doc "Provide your own cell renderer component to use for full width rows."}
     :full-width-cell-renderer-params {:camel "fullWidthCellRendererParams" :type :any :doc "Customise the parameters provided to the `fullWidthCellRenderer` component."}
     :functions-read-only {:camel "functionsReadOnly" :type :boolean :default "false" :doc "If `true`, then row group, pivot and value aggregation will be read-only from the GUI. The grid will display what values are used for each, but will not allow the user to change the selection."}
     :get-business-key-for-node {:camel "getBusinessKeyForNode" :type :object :doc "Return a business key for the node. If implemented, each row in the DOM will have an attribute `row-business-key='abc'` where `abc` is what you return as the business key."}
     :get-chart-toolbar-items {:camel "getChartToolbarItems" :type :object :initial? true :doc "Callback to be used to customise the chart toolbar items."}
     :get-child-count {:camel "getChildCount" :type :object :initial? true :doc "Allows setting the child count for a group row."}
     :get-context-menu-items {:camel "getContextMenuItems" :type :object :doc "For customising the context menu."}
     :get-data-path {:camel "getDataPath" :type :object :initial? true :doc "Callback to be used when working with Tree Data when `treeData = true`."}
     :get-document {:camel "getDocument" :type :object :doc "Allows overriding what `document` is used. Currently used by Drag and Drop (may extend to other places in the future). Use this when you want the grid to use a different `document` than the one available on the global scope. This can happen if docking out components (something which Electron supports)"}
     :get-full-row-edit-validation-errors {:camel "getFullRowEditValidationErrors" :type :object :doc "Validates the Full Row Edit. Only relevant when `editType=\"fullRow\"`."}
     :get-group-row-agg {:camel "getGroupRowAgg" :type :object :doc "Callback to use when you need access to more then the current column for aggregation."}
     :get-locale-text {:camel "getLocaleText" :type :object :initial? true :doc "A callback for localising text within the grid."}
     :get-main-menu-items {:camel "getMainMenuItems" :type :object :initial? true :doc "For customising the main 'column header' menu."}
     :get-row-class {:camel "getRowClass" :type :object :doc "Callback version of property `rowClass` to set class(es) for each row individually. Function should return either a string (class name), array of strings (array of class names) or undefined for no class."}
     :get-row-height {:camel "getRowHeight" :type :object :doc "Callback version of property `rowHeight` to set height for each row individually. Function should return a positive number of pixels, or return `null`/`undefined` to use the default row height."}
     :get-row-id {:camel "getRowId" :type :object :initial? true :doc "Provide a pure function that returns a string ID to uniquely identify a given row. This enables the grid to work optimally with data changes and updates."}
     :get-row-style {:camel "getRowStyle" :type :object :doc "Callback version of property `rowStyle` to set style for each row individually. Function should return an object of CSS values or undefined for no styles."}
     :get-server-side-group-key {:camel "getServerSideGroupKey" :type :object :doc "SSRM Tree Data: Allows specifying group keys."}
     :get-server-side-group-level-params {:camel "getServerSideGroupLevelParams" :type :object :initial? true :doc "Allows providing different params for different levels of grouping."}
     :grand-total-row {:camel "grandTotalRow" :type :object :doc "When provided, an extra grand total row will be inserted into the grid at the specified position."}
     :grid-id {:camel "gridId" :type :string :initial? true :doc "Provide a custom `gridId` for this instance of the grid. Value will be set on the root DOM node using the attribute `grid-id` as well as being accessible via the `gridApi.getGridId()` method."}
     :group-agg-filtering {:camel "groupAggFiltering" :type :object :default "false" :doc "Set to determine whether filters should be applied on aggregated group values."}
     :group-allow-unbalanced {:camel "groupAllowUnbalanced" :type :boolean :default "false" :doc "Set to `true` to prevent the grid from creating a '(Blanks)' group for nodes which do not belong to a group, and display the unbalanced nodes alongside group nodes."}
     :group-default-expanded {:camel "groupDefaultExpanded" :type :number :default "0" :doc "If grouping, set to the number of levels to expand by default, e.g. `0` for none, `1` for first level only, etc. Set to `-1` to expand everything."}
     :group-display-type {:camel "groupDisplayType" :type :object :doc "Specifies how the results of row grouping should be displayed."}
     :group-header-height {:camel "groupHeaderHeight" :type :number :doc "The height in pixels for the rows containing header column groups. If not specified, it uses `headerHeight`."}
     :group-hide-columns-until-expanded {:camel "groupHideColumnsUntilExpanded" :type :boolean :default "false" :doc "When using `groupDisplayType='multipleColumns'` or `groupHideOpenParents=true`, hides group columns for levels"}
     :group-hide-open-parents {:camel "groupHideOpenParents" :type :boolean :default "false" :doc "Set to `true` to hide parents that are open. When used with multiple columns for showing groups, it can give a more pleasing user experience."}
     :group-hide-parent-of-single-child {:camel "groupHideParentOfSingleChild" :type :object :default "false" :doc "Enable to display the child row in place of the group row when the group only has a single child."}
     :group-hierarchy-config {:camel "groupHierarchyConfig" :type :object :doc "Custom group hierarchy components can be defined here for later use in `colDef.groupHierarchy`"}
     :group-lock-group-columns {:camel "groupLockGroupColumns" :type :number :default "0" :initial? true :doc "If grouping, locks the group settings of a number of columns, e.g. `0` for no group locking. `1` for first group column locked, `-1` for all group columns locked."}
     :group-maintain-order {:camel "groupMaintainOrder" :type :boolean :default "false" :doc "When `true`, sorting on non-group columns does not reorder groups; only the rows within"}
     :group-remove-lowest-single-children {:camel "groupRemoveLowestSingleChildren" :type :boolean :default "false" :deprecated "v33.0.0 - use `groupHideParentOfSingleChild: 'leafGroupsOnly'` instead." :doc "Set to `true` to collapse lowest level groups that only have one child."}
     :group-remove-single-children {:camel "groupRemoveSingleChildren" :type :boolean :default "false" :deprecated "v33.0.0 - use `groupHideParentOfSingleChild` instead." :doc "Set to `true` to collapse groups that only have one child."}
     :group-row-renderer {:camel "groupRowRenderer" :type :any :doc "Provide the Cell Renderer to use when `groupDisplayType = 'groupRows'`."}
     :group-row-renderer-params {:camel "groupRowRendererParams" :type :any :doc "Customise the parameters provided to the `groupRowRenderer` component."}
     :group-selects-children {:camel "groupSelectsChildren" :type :boolean :default "false" :deprecated "v32.2 Use `rowSelection.groupSelects` instead" :doc "When `true`, if you select a group, the children of the group will also be selected."}
     :group-selects-filtered {:camel "groupSelectsFiltered" :type :boolean :default "false" :deprecated "v32.2 Use `rowSelection.groupSelects` instead" :doc "If using `groupSelectsChildren`, then only the children that pass the current filter will get selected."}
     :group-suppress-blank-header {:camel "groupSuppressBlankHeader" :type :boolean :default "false" :doc "If `true`, and showing footer, aggregate data will always be displayed at both the header and footer levels. This stops the possibly undesirable behaviour of the header details 'jumping' to the footer on expand."}
     :group-total-row {:camel "groupTotalRow" :type :object :doc "When provided, an extra row group total row will be inserted into row groups at the specified position, to display"}
     :header-height {:camel "headerHeight" :type :number :doc "The height in pixels for the row containing the column label header. If not specified, it uses the theme value of `header-height`."}
     :hide-padded-header-rows {:camel "hidePaddedHeaderRows" :type :boolean :doc "Hide any column header rows that would only contain padded groups."}
     :icons {:camel "icons" :type :object :initial? true :doc "Icons to use inside the grid instead of the grid's default icons."}
     :include-hidden-columns-in-advanced-filter {:camel "includeHiddenColumnsInAdvancedFilter" :type :boolean :default "false" :doc "Hidden columns are excluded from the Advanced Filter by default."}
     :include-hidden-columns-in-quick-filter {:camel "includeHiddenColumnsInQuickFilter" :type :boolean :default "false" :doc "Hidden columns are excluded from the Quick Filter by default."}
     :infinite-initial-row-count {:camel "infiniteInitialRowCount" :type :number :default "1" :initial? true :doc "How many extra blank rows to display to the user at the end of the dataset, which sets the vertical scroll and then allows the grid to request viewing more rows of data."}
     :initial-group-order-comparator {:camel "initialGroupOrderComparator" :type :object :doc "Allows default sorting of groups."}
     :initial-state {:camel "initialState" :type :object :initial? true :doc "Initial state for the grid. Only read once on initialization. Can be used in conjunction with `api.getState()` to save and restore grid state."}
     :invalid-edit-value-mode {:camel "invalidEditValueMode" :type :object :doc "Set to `block` to block the commit of invalid cell edits, keeping editors open."}
     :is-apply-server-side-transaction {:camel "isApplyServerSideTransaction" :type :object :doc "Allows cancelling transactions."}
     :is-external-filter-present {:camel "isExternalFilterPresent" :type :object :doc "Grid calls this method to know if an external filter is present."}
     :is-full-width-row {:camel "isFullWidthRow" :type :object :doc "Tells the grid if this row should be rendered as full width."}
     :is-group-open-by-default {:camel "isGroupOpenByDefault" :type :object :doc "(Client-side Row Model only) Allows groups to be open by default."}
     :is-row-master {:camel "isRowMaster" :type :object :doc "Callback to be used with Master Detail to determine if a row should be a master row. If `false` is returned no detail row will exist for this row."}
     :is-row-pinnable {:camel "isRowPinnable" :type :object :doc "Return `true` if the grid should allow the row to be manually pinned."}
     :is-row-pinned {:camel "isRowPinned" :type :object :doc "Called for every row in the grid."}
     :is-row-selectable {:camel "isRowSelectable" :type :object :deprecated "v32.2 Use `rowSelection.isRowSelectable` instead" :doc "Callback to be used to determine which rows are selectable. By default rows are selectable, so return `false` to make a row un-selectable."}
     :is-row-valid-drop-position {:camel "isRowValidDropPosition" :type :object :doc "Called by drag and drop when rows are dragged over another row to conditionally prevent dropping the dragged row on the hovered row."}
     :is-server-side-group {:camel "isServerSideGroup" :type :object :doc "SSRM Tree Data: Allows specifying which rows are expandable."}
     :is-server-side-group-open-by-default {:camel "isServerSideGroupOpenByDefault" :type :object :doc "Allows groups to be open by default."}
     :keep-detail-rows {:camel "keepDetailRows" :type :boolean :default "false" :initial? true :doc "Set to `true` to keep detail rows for when they are displayed again."}
     :keep-detail-rows-count {:camel "keepDetailRowsCount" :type :number :default "10" :initial? true :doc "Sets the number of details rows to keep."}
     :load-theme-google-fonts {:camel "loadThemeGoogleFonts" :type :boolean :doc "If your theme uses a font that is available on Google Fonts, pass true to load it from Google's CDN."}
     :loading {:camel "loading" :type :boolean :default "undefined" :doc "Show or hide the loading overlay."}
     :loading-cell-renderer {:camel "loadingCellRenderer" :type :any :doc "Provide your own loading cell renderer to use when data is loading via a DataSource or when a cell renderer is deferred."}
     :loading-cell-renderer-params {:camel "loadingCellRendererParams" :type :any :doc "Params to be passed to the `loadingCellRenderer` component."}
     :loading-cell-renderer-selector {:camel "loadingCellRendererSelector" :type :object :initial? true :doc "Callback to select which loading cell renderer to be used when data is loading via a DataSource or when a cell renderer is deferred."}
     :loading-overlay-component {:camel "loadingOverlayComponent" :type :any :doc "Provide a custom loading overlay component."}
     :loading-overlay-component-params {:camel "loadingOverlayComponentParams" :type :any :doc "Customise the parameters provided to the loading overlay component."}
     :locale-text {:camel "localeText" :type :object :initial? true :doc "A map of key->value pairs for localising text within the grid."}
     :maintain-column-order {:camel "maintainColumnOrder" :type :boolean :default "false" :doc "Keeps the order of Columns maintained after new Column Definitions are updated."}
     :master-detail {:camel "masterDetail" :type :boolean :default "false" :doc "Set to `true` to enable Master Detail."}
     :max-blocks-in-cache {:camel "maxBlocksInCache" :type :number :initial? true :doc "How many blocks to keep in the store. Default is no limit, so every requested block is kept. Use this if you have memory concerns, and blocks that were least recently viewed will be purged when the limit is hit. The grid will additionally make sure it has all the blocks needed to display what is currently visible, in case this property is set to a low value."}
     :max-concurrent-datasource-requests {:camel "maxConcurrentDatasourceRequests" :type :number :default "2" :initial? true :doc "How many requests to hit the server with concurrently. If the max is reached, requests are queued."}
     :multi-sort-key {:camel "multiSortKey" :type :object :doc "Set to `'ctrl'` to have multi sorting by clicking work using the `Ctrl` (or `Command ⌘` for Mac) key."}
     :navigate-to-next-cell {:camel "navigateToNextCell" :type :object :doc "Allows overriding the default behaviour for when user hits navigation (arrow) key when a cell is focused. Return the next Cell position to navigate to or `null` to stay on current cell."}
     :navigate-to-next-header {:camel "navigateToNextHeader" :type :object :doc "Allows overriding the default behaviour for when user hits navigation (arrow) key when a header is focused. Return the next Header position to navigate to or `null` to stay on current header."}
     :no-rows-overlay-component {:camel "noRowsOverlayComponent" :type :any :doc "Provide a custom no-rows overlay component."}
     :no-rows-overlay-component-params {:camel "noRowsOverlayComponentParams" :type :any :doc "Customise the parameters provided to the no-rows overlay component."}
     :note-hide-delay {:camel "noteHideDelay" :type :number :default "220" :doc "The delay in milliseconds before a note is hidden after the pointer leaves a noted cell or note popup."}
     :note-show-delay {:camel "noteShowDelay" :type :number :default "180" :doc "The delay in milliseconds before a note is shown when hovering a noted cell."}
     :note-trigger {:camel "noteTrigger" :type :object :default "'hover'" :doc "Changes how existing notes are opened."}
     :notes-data-source {:camel "notesDataSource" :type :object :doc "Provide a data source to control where notes are stored and retrieved."}
     :overlay-component {:camel "overlayComponent" :type :any :initial? true :doc "Provide a custom overlay component to be used for all grid provided overlays (loading, no rows, no matching rows, exporting etc)."}
     :overlay-component-params {:camel "overlayComponentParams" :type :any :doc "Customise the parameters provided to the `overlayComponent`."}
     :overlay-component-selector {:camel "overlayComponentSelector" :type :object :initial? true :doc "Callback to dynamically provide a custom overlay component complete with custom params based on the selector params."}
     :overlay-loading-template {:camel "overlayLoadingTemplate" :type :string :doc "Provide a HTML string to override the default loading overlay. Supports non-empty plain text or HTML with a single root element."}
     :overlay-no-rows-template {:camel "overlayNoRowsTemplate" :type :string :doc "Provide a HTML string to override the default no-rows overlay. Supports non-empty plain text or HTML with a single root element."}
     :paginate-child-rows {:camel "paginateChildRows" :type :boolean :default "false" :initial? true :doc "Set to `true` to have pages split children of groups when using Row Grouping or detail rows with Master Detail."}
     :pagination {:camel "pagination" :type :boolean :default "false" :doc "Set whether pagination is enabled."}
     :pagination-auto-page-size {:camel "paginationAutoPageSize" :type :boolean :default "false" :doc "Set to `true` so that the number of rows to load per page is automatically adjusted by the grid so each page shows enough rows to just fill the area designated for the grid. If `false`, `paginationPageSize` is used."}
     :pagination-number-formatter {:camel "paginationNumberFormatter" :type :object :initial? true :doc "Allows user to format the numbers in the pagination panel, i.e. 'row count' and 'page number' labels. This is for pagination panel only, to format numbers inside the grid's cells (i.e. your data), then use `valueFormatter` in the column definitions."}
     :pagination-page-size {:camel "paginationPageSize" :type :number :default "100" :doc "How many rows to load per page. If `paginationAutoPageSize` is specified, this property is ignored."}
     :pagination-page-size-selector {:camel "paginationPageSizeSelector" :type :array :default "true" :doc "Determines if the page size selector is shown in the pagination panel or not."}
     :pagination-panels {:camel "paginationPanels" :type :array :doc "Controls which built-in components appear in the pagination panel and in what order."}
     :pinned-bottom-row-data {:camel "pinnedBottomRowData" :type :array :doc "Data to be displayed as pinned bottom rows in the grid."}
     :pinned-top-row-data {:camel "pinnedTopRowData" :type :array :doc "Data to be displayed as pinned top rows in the grid."}
     :pivot-column-group-totals {:camel "pivotColumnGroupTotals" :type :object :doc "When set and the grid is in pivot mode, automatically calculated totals will appear within the Pivot Column Groups, in the position specified."}
     :pivot-default-expanded {:camel "pivotDefaultExpanded" :type :number :default "0" :doc "If pivoting, set to the number of column group levels to expand by default, e.g. `0` for none, `1` for first level only, etc. Set to `-1` to expand everything."}
     :pivot-group-header-height {:camel "pivotGroupHeaderHeight" :type :number :doc "The height in pixels for the row containing header column groups when in pivot mode. If not specified, it uses `groupHeaderHeight`."}
     :pivot-header-height {:camel "pivotHeaderHeight" :type :number :doc "The height in pixels for the row containing the columns when in pivot mode. If not specified, it uses `headerHeight`."}
     :pivot-max-generated-columns {:camel "pivotMaxGeneratedColumns" :type :number :default "-1" :doc "The maximum number of generated columns before the grid halts execution. Upon reaching this number, the grid halts generation of columns"}
     :pivot-mode {:camel "pivotMode" :type :boolean :default "false" :doc "Set to `true` to enable pivot mode."}
     :pivot-panel-show {:camel "pivotPanelShow" :type :object :default "'never'" :initial? true :doc "When to show the 'pivot panel' (where you drag rows to pivot) at the top. Note that the pivot panel will never show if `pivotMode` is off."}
     :pivot-row-totals {:camel "pivotRowTotals" :type :object :doc "When set and the grid is in pivot mode, automatically calculated totals will appear for each value column in the position specified."}
     :pivot-suppress-auto-column {:camel "pivotSuppressAutoColumn" :type :boolean :default "false" :initial? true :doc "If `true`, the grid will not swap in the grouping column when pivoting. Useful if pivoting using Server Side Row Model or Viewport Row Model and you want full control of all columns including the group column."}
     :popup-parent {:camel "popupParent" :type :object :doc "DOM element to use as the popup parent for grid popups (context menu, column menu etc)."}
     :post-process-popup {:camel "postProcessPopup" :type :object :doc "Allows user to process popups after they are created. Applications can use this if they want to, for example, reposition the popup."}
     :post-sort-rows {:camel "postSortRows" :type :object :doc "Callback to perform additional sorting after the grid has sorted the rows."}
     :prevent-default-on-context-menu {:camel "preventDefaultOnContextMenu" :type :boolean :default "false" :doc "When using `suppressContextMenu`, you can use the `onCellContextMenu` function to provide your own code to handle cell `contextmenu` events."}
     :process-auto-generated-column-defs {:camel "processAutoGeneratedColumnDefs" :type :object :doc "Callback fired after auto-generating column definitions and before they are applied to the grid."}
     :process-cell-for-clipboard {:camel "processCellForClipboard" :type :object :doc "Allows you to process cells for the clipboard. Handy if for example you have `Date` objects that need to have a particular format if importing into Excel."}
     :process-cell-from-clipboard {:camel "processCellFromClipboard" :type :object :doc "Allows you to process cells from the clipboard. Handy if for example you have number fields and want to block non-numbers from getting into the grid."}
     :process-data-from-clipboard {:camel "processDataFromClipboard" :type :object :doc "Allows complete control of the paste operation, including cancelling the operation (so nothing happens) or replacing the data with other data."}
     :process-file-input {:camel "processFileInput" :type :function :doc "Callback to handle files received via the file input overlay (drag-and-drop or file browser)."}
     :process-group-header-for-clipboard {:camel "processGroupHeaderForClipboard" :type :object :doc "Allows you to process group header values for the clipboard."}
     :process-header-for-clipboard {:camel "processHeaderForClipboard" :type :object :doc "Allows you to process header values for the clipboard."}
     :process-pivot-result-col-def {:camel "processPivotResultColDef" :type :object :doc "Callback for the mutation of the generated pivot result column definitions"}
     :process-pivot-result-col-group-def {:camel "processPivotResultColGroupDef" :type :object :doc "Callback for the mutation of the generated pivot result column group definitions"}
     :process-row-post-create {:camel "processRowPostCreate" :type :object :doc "Callback fired after the row is rendered into the DOM. Should not be used to initiate side effects."}
     :process-unpinned-columns {:camel "processUnpinnedColumns" :type :object :initial? true :doc "Allows the user to process the columns being removed from the pinned section because the viewport is too small to accommodate them."}
     :purge-closed-row-nodes {:camel "purgeClosedRowNodes" :type :boolean :default "false" :doc "When enabled, closing group rows will remove children of that row. Next time the row is opened, child rows will be read from the datasource again. This property only applies when there is Row Grouping or Tree Data."}
     :quick-filter-matcher {:camel "quickFilterMatcher" :type :object :doc "Changes the matching logic for whether a row passes the Quick Filter."}
     :quick-filter-parser {:camel "quickFilterParser" :type :object :doc "Changes how the Quick Filter splits the Quick Filter text into search terms."}
     :quick-filter-text {:camel "quickFilterText" :type :string :doc "Rows are filtered using this text as a Quick Filter."}
     :reactive-custom-components {:camel "reactiveCustomComponents" :type :boolean :default "true" :initial? true :deprecated "As of v32 custom components are created reactively by default. Set this property to `false` to switch to the legacy way of declaring custom components imperatively." :doc "**React only**."}
     :read-only-edit {:camel "readOnlyEdit" :type :boolean :default "false" :doc "Set to `true` to stop the grid updating data after `Edit`, `Clipboard` and `Fill Handle` operations. When this is set, it is intended the application will update the data, eg in an external immutable store, and then pass the new dataset to the grid. <br />**Note:** `rowNode.setDataValue()` does not update the value of the cell when this is `True`, it fires `onCellEditRequest` instead."}
     :refresh-after-group-edit {:camel "refreshAfterGroupEdit" :type :boolean :default "false" :doc "When `true`, the grid re-evaluates the grouping hierarchy after editing a grouped column value,"}
     :remove-pivot-header-row-when-single-value-column {:camel "removePivotHeaderRowWhenSingleValueColumn" :type :boolean :default "false" :doc "Set to `true` to omit the value Column header when there is only a single value column."}
     :rendering-mode {:camel "renderingMode" :type :object :default "'default'" :doc "** React only**."}
     :reset-row-data-on-update {:camel "resetRowDataOnUpdate" :type :boolean :default "false" :doc "When enabled, getRowId() callback is implemented and new Row Data is set, the grid will disregard all previous rows and treat the new Row Data as new data. As a consequence, all Row State (eg selection, rendered rows) will be reset."}
     :row-buffer {:camel "rowBuffer" :type :number :default "10" :doc "The number of rows rendered outside the viewable area the grid renders."}
     :row-class {:camel "rowClass" :type :array :doc "CSS class(es) for all rows. Provide either a string (class name) or array of strings (array of class names)."}
     :row-class-rules {:camel "rowClassRules" :type :object :doc "Rules which can be applied to include certain CSS classes."}
     :row-data {:camel "rowData" :type :array :doc "Set the data to be displayed as rows in the grid."}
     :row-drag-entire-row {:camel "rowDragEntireRow" :type :boolean :default "false" :doc "Set to `true` to enable clicking and dragging anywhere on the row without the need for a drag handle."}
     :row-drag-insert-delay {:camel "rowDragInsertDelay" :type :number :default "500" :doc "Used if rowDragManaged is enabled and treeData is enabled,"}
     :row-drag-managed {:camel "rowDragManaged" :type :boolean :default "false" :doc "Set to `true` to enable Managed Row Dragging."}
     :row-drag-multi-row {:camel "rowDragMultiRow" :type :boolean :default "false" :doc "Set to `true` to enable dragging multiple rows at the same time."}
     :row-drag-text {:camel "rowDragText" :type :object :initial? true :doc "A callback that should return a string to be displayed by the `rowDragComp` while dragging a row."}
     :row-group-panel-show {:camel "rowGroupPanelShow" :type :object :default "'never'" :doc "When to show the 'row group panel' (where you drag rows to group) at the top."}
     :row-group-panel-suppress-sort {:camel "rowGroupPanelSuppressSort" :type :boolean :default "false" :doc "Set to `true` to suppress sort indicators and actions from the row group panel."}
     :row-height {:camel "rowHeight" :type :number :default "25" :doc "Default row height in pixels."}
     :row-model-type {:camel "rowModelType" :type :object :default "'clientSide'" :initial? true :doc "Sets the row model type."}
     :row-multi-select-with-click {:camel "rowMultiSelectWithClick" :type :boolean :default "false" :deprecated "v32.2 Use `rowSelection.enableSelectionWithoutKeys` instead" :doc "Set to `true` to allow multiple rows to be selected using single click."}
     :row-numbers {:camel "rowNumbers" :type :object :default "false" :doc "Configure the Row Numbers Feature."}
     :row-selection {:camel "rowSelection" :type :object :doc "Use the `RowSelectionOptions` object to configure row selection. The string values `'single'` and `'multiple'` are deprecated."}
     :row-style {:camel "rowStyle" :type :object :doc "The style properties to apply to all rows. Set to an object of key (style names) and values (style values)."}
     :scrollbar-width {:camel "scrollbarWidth" :type :number :initial? true :doc "Tell the grid how wide in pixels the scrollbar is, which is used in grid width calculations. Set only if using non-standard browser-provided scrollbars, so the grid can use the non-standard size in its calculations."}
     :selection-column-def {:camel "selectionColumnDef" :type :object :doc "Configure the selection column, used for displaying checkboxes."}
     :send-to-clipboard {:camel "sendToClipboard" :type :object :doc "Allows you to get the data that would otherwise go to the clipboard. To be used when you want to control the 'copy to clipboard' operation yourself."}
     :server-side-datasource {:camel "serverSideDatasource" :type :object :doc "Provide the `serverSideDatasource` for server side row model."}
     :server-side-enable-client-side-sort {:camel "serverSideEnableClientSideSort" :type :boolean :default "false" :doc "When enabled, sorts fully loaded groups in the browser instead of requesting from the server."}
     :server-side-initial-row-count {:camel "serverSideInitialRowCount" :type :number :default "1" :initial? true :doc "Set how many loading rows to display to the user for the root level group."}
     :server-side-only-refresh-filtered-groups {:camel "serverSideOnlyRefreshFilteredGroups" :type :boolean :default "false" :initial? true :doc "When enabled, only refresh groups directly impacted by a filter. This property only applies when there is Row Grouping & filtering is handled on the server."}
     :server-side-pivot-result-field-separator {:camel "serverSidePivotResultFieldSeparator" :type :string :default "'_'" :initial? true :doc "Used to split pivot field strings for generating pivot result columns when `pivotResultFields` is provided as part of a `getRows` success."}
     :server-side-sort-all-levels {:camel "serverSideSortAllLevels" :type :boolean :default "false" :doc "When enabled, always refreshes top level groups regardless of which column was sorted. This property only applies when there is Row Grouping & sorting is handled on the server."}
     :show-opened-group {:camel "showOpenedGroup" :type :boolean :default "false" :doc "Shows the open group in the group column for non-group rows."}
     :side-bar {:camel "sideBar" :type :array :doc "Specifies the side bar components."}
     :single-click-edit {:camel "singleClickEdit" :type :boolean :default "false" :doc "Set to `true` to enable Single Click Editing for cells, to start editing with a single click."}
     :skip-header-on-auto-size {:camel "skipHeaderOnAutoSize" :type :boolean :default "false" :initial? true :doc "Set this to `true` to skip the `headerName` when `autoSize` is called by default."}
     :sorting-order {:camel "sortingOrder" :type :array :default "[null, 'asc', 'desc']" :deprecated "v33 Use `defaultColDef.sortingOrder` instead" :doc "Array defining the order in which sorting occurs (if sorting is enabled). Values can be `'asc'`, `'desc'` or `null`. For example: `sortingOrder: ['asc', 'desc']`."}
     :ssrm-expand-all-affects-all-rows {:camel "ssrmExpandAllAffectsAllRows" :type :boolean :doc "Controls how expand/collapse operations affect all rows and group interactions."}
     :status-bar {:camel "statusBar" :type :object :doc "Specifies the status bar components to use in the status bar."}
     :stop-editing-when-cells-lose-focus {:camel "stopEditingWhenCellsLoseFocus" :type :boolean :default "false" :initial? true :doc "Set this to `true` to stop cell editing when grid loses focus."}
     :style-nonce {:camel "styleNonce" :type :string :doc "The nonce attribute to set on style elements added to the document by"}
     :suppress-advanced-filter-eval {:camel "suppressAdvancedFilterEval" :type :boolean :default "true" :deprecated "As of v34, advanced filter no longer uses function evaluation, so this option has no effect."}
     :suppress-agg-filtered-only {:camel "suppressAggFilteredOnly" :type :boolean :default "false" :doc "Set to `true` so that aggregations are not impacted by filtering."}
     :suppress-agg-func-in-header {:camel "suppressAggFuncInHeader" :type :boolean :default "false" :doc "When `true`, column headers won't include the `aggFunc` name, e.g. `'sum(Bank Balance)`' will just be `'Bank Balance'`."}
     :suppress-animation-frame {:camel "suppressAnimationFrame" :type :boolean :default "false" :initial? true :doc "When `true`, the grid will not use animation frames when drawing rows while scrolling. Use this if and only if the grid is working fast enough on all users machines and you want to avoid the temporarily empty rows."}
     :suppress-auto-size {:camel "suppressAutoSize" :type :boolean :default "false" :initial? true :doc "Suppresses auto-sizing columns for columns. In other words, double clicking a column's header's edge will not auto-size."}
     :suppress-browser-resize-observer {:camel "suppressBrowserResizeObserver" :type :boolean :default "false" :initial? true :deprecated "As of v32.2 the grid always uses the browser's ResizeObserver, this grid option has no effect"}
     :suppress-cell-focus {:camel "suppressCellFocus" :type :boolean :default "false" :doc "If `true`, cells won't be focusable. This means keyboard navigation will be disabled for grid cells, but remain enabled in other elements of the grid such as column headers, floating filters, tool panels."}
     :suppress-change-detection {:camel "suppressChangeDetection" :type :boolean :default "false" :doc "Disables change detection."}
     :suppress-clear-on-fill-reduction {:camel "suppressClearOnFillReduction" :type :boolean :default "false" :deprecated "v32.2 Use `cellSelection.suppressClearOnFillReduction` instead" :doc "Set this to `true` to prevent cell values from being cleared when the Range Selection is reduced by the Fill Handle."}
     :suppress-click-edit {:camel "suppressClickEdit" :type :boolean :default "false" :doc "Set to `true` so that neither single nor double click starts editing."}
     :suppress-clipboard-api {:camel "suppressClipboardApi" :type :boolean :default "false" :doc "Set to `true` to stop the grid trying to use the Clipboard API, if it is blocked, and immediately fallback to the workaround."}
     :suppress-clipboard-paste {:camel "suppressClipboardPaste" :type :boolean :default "false" :doc "Set to `true` to turn off paste operations within the grid."}
     :suppress-column-move-animation {:camel "suppressColumnMoveAnimation" :type :boolean :default "false" :doc "If `true`, the `ag-column-moving` class is not added to the grid while columns are moving. In the default themes, this results in no animation when moving columns."}
     :suppress-column-virtualisation {:camel "suppressColumnVirtualisation" :type :boolean :default "false" :initial? true :doc "Set to `true` so that the grid doesn't virtualise the columns. For example, if you have 100 columns, but only 10 visible due to scrolling, all 100 will always be rendered."}
     :suppress-content-visibility-auto {:camel "suppressContentVisibilityAuto" :type :boolean :default "true" :initial? true :doc "Set to `false` to enable `content-visibility: auto` on the grid wrapper element. This improves performance by allowing the browser to skip rendering grids that are off screen, but may cause issues if your application depends on receiving resize events from hidden grids."}
     :suppress-context-menu {:camel "suppressContextMenu" :type :boolean :default "false" :doc "Set to `true` to not show the context menu. Use if you don't want to use the default 'right click' context menu."}
     :suppress-copy-rows-to-clipboard {:camel "suppressCopyRowsToClipboard" :type :boolean :default "false" :deprecated "v32.2 Use `rowSelection.copySelectedRows` instead." :doc "Set to `true` to copy the cell range or focused cell to the clipboard and never the selected rows."}
     :suppress-copy-single-cell-ranges {:camel "suppressCopySingleCellRanges" :type :boolean :default "false" :deprecated "v32.2 Use `rowSelection.copySelectedRows` instead." :doc "Set to `true` to copy rows instead of ranges when a range with only a single cell is selected."}
     :suppress-csv-export {:camel "suppressCsvExport" :type :boolean :default "false" :doc "Prevents the user from exporting the grid to CSV."}
     :suppress-cut-to-clipboard {:camel "suppressCutToClipboard" :type :boolean :default "false" :doc "Set to `true` to block **cut** operations within the grid."}
     :suppress-drag-leave-hides-columns {:camel "suppressDragLeaveHidesColumns" :type :boolean :default "false" :doc "If `true`, when you drag a column out of the grid (e.g. to the group zone) the column is not hidden."}
     :suppress-excel-export {:camel "suppressExcelExport" :type :boolean :default "false" :doc "Prevents the user from exporting the grid to Excel."}
     :suppress-expandable-pivot-groups {:camel "suppressExpandablePivotGroups" :type :boolean :default "false" :initial? true :doc "When enabled, pivot column groups will appear 'fixed', without the ability to expand and collapse the column groups."}
     :suppress-field-dot-notation {:camel "suppressFieldDotNotation" :type :boolean :default "false" :doc "If `true`, then dots in field names (e.g. `'address.firstLine'`) are not treated as deep references. Allows you to use dots in your field name if you prefer."}
     :suppress-focus-after-refresh {:camel "suppressFocusAfterRefresh" :type :boolean :default "false" :doc "Set to `true` to not set focus back on the grid after a refresh. This can avoid issues where you want to keep the focus on another part of the browser."}
     :suppress-group-changes-column-visibility {:camel "suppressGroupChangesColumnVisibility" :type :object :default "false" :doc "Enable to prevent column visibility changing when grouped columns are changed."}
     :suppress-group-rows-sticky {:camel "suppressGroupRowsSticky" :type :boolean :default "false" :initial? true :doc "Set to `true` prevent Group Rows from sticking to the top of the grid."}
     :suppress-header-focus {:camel "suppressHeaderFocus" :type :boolean :default "false" :doc "If `true`, header cells won't be focusable. This means keyboard navigation will be disabled for grid header cells, but remain enabled in other elements of the grid such as grid cells and tool panels."}
     :suppress-horizontal-scroll {:camel "suppressHorizontalScroll" :type :boolean :default "false" :doc "Set to `true` to never show the horizontal scroll. This is useful if the grid is aligned with another grid and will scroll when the other grid scrolls. (Should not be used in combination with `alwaysShowHorizontalScroll`.)"}
     :suppress-last-empty-line-on-paste {:camel "suppressLastEmptyLineOnPaste" :type :boolean :default "false" :doc "Set to `true` to work around a bug with Excel (Windows) that adds an extra empty line at the end of ranges copied to the clipboard."}
     :suppress-loading-overlay {:camel "suppressLoadingOverlay" :type :boolean :default "false" :initial? true :deprecated "v32 - Deprecated. Use `suppressOverlays=['loading']` or `loading=false` instead." :doc "Disables the 'loading' overlay."}
     :suppress-maintain-unsorted-order {:camel "suppressMaintainUnsortedOrder" :type :boolean :default "false" :doc "Set to `true` to suppress sorting of un-sorted data to match original row data."}
     :suppress-make-column-visible-after-un-group {:camel "suppressMakeColumnVisibleAfterUnGroup" :type :boolean :default "false" :deprecated "v33.0.0 - Use `suppressGroupChangesColumnVisibility: 'suppressShowOnUngroup'` instead." :doc "By default, when a column is un-grouped, i.e. using the Row Group Panel, it is made visible in the grid. This property stops the column becoming visible again when un-grouping."}
     :suppress-max-rendered-row-restriction {:camel "suppressMaxRenderedRowRestriction" :type :boolean :default "false" :initial? true :doc "By default the grid has a limit of rendering a maximum of 500 rows at once (remember the grid only renders rows you can see, so unless your display shows more than 500 rows without vertically scrolling this will never be an issue)."}
     :suppress-menu-hide {:camel "suppressMenuHide" :type :boolean :default "true" :doc "Only recommended for use if `columnMenu = 'legacy'`."}
     :suppress-middle-click-scrolls {:camel "suppressMiddleClickScrolls" :type :boolean :default "false" :doc "If `true`, middle clicks will result in `click` events for cells and rows. Otherwise the browser will use middle click to scroll the grid.<br />**Note:** Not all browsers fire `click` events with the middle button. Most will fire only `mousedown` and `mouseup` events, which can be used to focus a cell, but will not work to call the `onCellClicked` function."}
     :suppress-model-update-after-update-transaction {:camel "suppressModelUpdateAfterUpdateTransaction" :type :boolean :default "false" :doc "Prevents Transactions changing sort, filter, group or pivot state when transaction only contains updates."}
     :suppress-movable-columns {:camel "suppressMovableColumns" :type :boolean :default "false" :doc "Set to `true` to suppress column moving, i.e. to make the columns fixed position."}
     :suppress-move-when-column-dragging {:camel "suppressMoveWhenColumnDragging" :type :boolean :default "false" :doc "Set to `true` to suppress moving columns while dragging the Column Header. This option highlights the position where the column will be placed and it will only move it on mouse up."}
     :suppress-move-when-row-dragging {:camel "suppressMoveWhenRowDragging" :type :boolean :default "false" :doc "Set to `true` to suppress moving rows while dragging the `rowDrag` waffle. This option highlights the position where the row will be placed and it will only move the row on mouse up."}
     :suppress-multi-range-selection {:camel "suppressMultiRangeSelection" :type :boolean :default "false" :deprecated "v32.2 Use `cellSelection.suppressMultiRanges` instead" :doc "If `true`, only a single range can be selected."}
     :suppress-multi-sort {:camel "suppressMultiSort" :type :boolean :default "false" :doc "Set to `true` to suppress multi-sort when the user shift-clicks a column header."}
     :suppress-no-rows-overlay {:camel "suppressNoRowsOverlay" :type :boolean :default "false" :initial? true :doc "Set to `true` to prevent the no-rows overlay being shown when there is no row data."}
     :suppress-overlays {:camel "suppressOverlays" :type :array :doc "List of provided overlay names to suppress. One of `loading`, `noRows`, `noMatchingRows`, `exporting`, `fileInput`."}
     :suppress-pagination-panel {:camel "suppressPaginationPanel" :type :boolean :default "false" :doc "If `true`, the default grid controls for navigation are hidden."}
     :suppress-prevent-default-on-mouse-wheel {:camel "suppressPreventDefaultOnMouseWheel" :type :boolean :default "false" :initial? true :doc "If `true`, mouse wheel events will be passed to the browser. Useful if your grid has no vertical scrolls and you want the mouse to scroll the browser page."}
     :suppress-property-names-check {:camel "suppressPropertyNamesCheck" :type :boolean :default "false" :initial? true :deprecated "As of v33 `gridOptions` and `columnDefs` both have a `context` property that should be used for arbitrary user data. This means that column definitions and gridOptions should only contain valid properties making this property redundant."}
     :suppress-row-click-selection {:camel "suppressRowClickSelection" :type :boolean :default "false" :deprecated "v32.2 Use `rowSelection.enableClickSelection` instead" :doc "If `true`, row selection won't happen when rows are clicked. Use when you only want checkbox selection."}
     :suppress-row-deselection {:camel "suppressRowDeselection" :type :boolean :default "false" :deprecated "v32.2 Use `rowSelection.enableClickSelection` instead" :doc "If `true`, rows will not be deselected if you hold down `Ctrl` and click the row or press `Space`."}
     :suppress-row-drag {:camel "suppressRowDrag" :type :boolean :default "false" :doc "Set to `true` to suppress row dragging."}
     :suppress-row-group-hides-columns {:camel "suppressRowGroupHidesColumns" :type :boolean :default "false" :deprecated "v33.0.0 - Use `suppressGroupChangesColumnVisibility: 'suppressHideOnGroup'` instead." :doc "If `true`, when you drag a column into a row group panel the column is not hidden."}
     :suppress-row-hover-highlight {:camel "suppressRowHoverHighlight" :type :boolean :default "false" :doc "Set to `true` to not highlight rows by adding the `ag-row-hover` CSS class."}
     :suppress-row-transform {:camel "suppressRowTransform" :type :boolean :default "false" :initial? true :doc "Uses CSS `top` instead of CSS `transform` for positioning rows. Useful if the transform function is causing issues such as used in row spanning."}
     :suppress-row-virtualisation {:camel "suppressRowVirtualisation" :type :boolean :default "false" :initial? true :doc "Set to `true` so that the grid doesn't virtualise the rows. For example, if you have 100 rows, but only 10 visible due to scrolling, all 100 will always be rendered."}
     :suppress-scroll-on-new-data {:camel "suppressScrollOnNewData" :type :boolean :default "false" :doc "When `true`, the grid will not scroll to the top when new row data is provided. Use this if you don't want the default behaviour of scrolling to the top every time you load new data."}
     :suppress-scroll-when-popups-are-open {:camel "suppressScrollWhenPopupsAreOpen" :type :boolean :default "false" :doc "When `true`, the grid will not allow mousewheel / touchpad scroll when popup elements are present."}
     :suppress-server-side-full-width-loading-row {:camel "suppressServerSideFullWidthLoadingRow" :type :boolean :doc "When `true`, the Server-side Row Model will not use a full width loading renderer, instead using the colDef `loadingCellRenderer` if present."}
     :suppress-set-filter-by-default {:camel "suppressSetFilterByDefault" :type :boolean :default "false" :initial? true :doc "When using AG Grid Enterprise, the Set Filter is used by default when `filter: true` is set on column definitions."}
     :suppress-start-edit-on-tab {:camel "suppressStartEditOnTab" :type :boolean :doc "Determine the behavior when navigating to the next/previous editable cell. Default is to begin editing the cell."}
     :suppress-sticky-total-row {:camel "suppressStickyTotalRow" :type :object :doc "Suppress the sticky behaviour of the total rows, can be suppressed individually by passing `'grand'` or `'group'`."}
     :suppress-touch {:camel "suppressTouch" :type :boolean :default "false" :initial? true :doc "Disables touch support (but does not remove the browser's efforts to simulate mouse events on touch)."}
     :tab-index {:camel "tabIndex" :type :number :default "0" :initial? true :doc "Change this value to set the tabIndex order of the Grid within your application."}
     :tab-to-next-cell {:camel "tabToNextCell" :type :object :doc "Allows overriding the default behaviour for when user hits `Tab` key when a cell is focused."}
     :tab-to-next-grid-container {:camel "tabToNextGridContainer" :type :object :doc "Allows overriding the default behaviour when tabbing between core grid containers."}
     :tab-to-next-header {:camel "tabToNextHeader" :type :object :doc "Allows overriding the default behaviour for when user hits `Tab` key when a header is focused."}
     :theme {:camel "theme" :type :object :default "themeQuartz" :doc "Theme to apply to the grid, or the string \"legacy\" to opt back into the"}
     :theme-css-layer {:camel "themeCssLayer" :type :string :doc "The CSS layer that this theme should be rendered onto. When specified,"}
     :theme-style-container {:camel "themeStyleContainer" :type :function :initial? true :doc "An element to insert style elements into when injecting styles into the"}
     :toolbar {:camel "toolbar" :type :object :doc "Specifies the toolbar items to use in the toolbar."}
     :tooltip-hide-delay {:camel "tooltipHideDelay" :type :number :default "10000" :doc "The delay in milliseconds that it takes for tooltips to hide once they have been displayed."}
     :tooltip-interaction {:camel "tooltipInteraction" :type :boolean :default "false" :initial? true :doc "Set to `true` to enable tooltip interaction. When this option is enabled, the tooltip will not hide while the"}
     :tooltip-mouse-track {:camel "tooltipMouseTrack" :type :boolean :default "false" :initial? true :doc "Set to `true` to have tooltips follow the cursor once they are displayed."}
     :tooltip-show-delay {:camel "tooltipShowDelay" :type :number :default "2000" :doc "The delay in milliseconds that it takes for tooltips to show up once an element is hovered over."}
     :tooltip-show-mode {:camel "tooltipShowMode" :type :object :default "`standard`" :doc "This defines when tooltip will show up for Cells, Headers and SetFilter Items."}
     :tooltip-switch-show-delay {:camel "tooltipSwitchShowDelay" :type :number :default "200" :doc "The delay in milliseconds before a tooltip is shown when moving the pointer from one tooltip-enabled element to"}
     :tooltip-trigger {:camel "tooltipTrigger" :type :object :default "'hover'" :initial? true :doc "The trigger that will cause tooltips to show and hide."}
     :tree-data {:camel "treeData" :type :boolean :default "false" :doc "Set to `true` to enable the Grid to work with Tree Data."}
     :tree-data-children-field {:camel "treeDataChildrenField" :type :string :doc "The name of the field to use in a data item to retrieve the array of children nodes of a node when while using treeData=true."}
     :tree-data-display-type {:camel "treeDataDisplayType" :type :object :doc "Specifies how tree data should be displayed."}
     :tree-data-parent-id-field {:camel "treeDataParentIdField" :type :string :doc "The name of the field to use in a data item to find the parent node of a node when using treeData=true."}
     :un-sort-icon {:camel "unSortIcon" :type :boolean :default "false" :deprecated "v33 Use `defaultColDef.unSortIcon` instead" :doc "Set to `true` to show the 'no sort' icon."}
     :undo-redo-cell-editing {:camel "undoRedoCellEditing" :type :boolean :initial? true :doc "Set to `true` to enable Undo / Redo while editing."}
     :undo-redo-cell-editing-limit {:camel "undoRedoCellEditingLimit" :type :number :default "10" :initial? true :doc "Set the size of the undo / redo stack."}
     :value-cache {:camel "valueCache" :type :boolean :default "false" :initial? true :doc "Set to `true` to turn on the value cache."}
     :value-cache-never-expires {:camel "valueCacheNeverExpires" :type :boolean :default "false" :initial? true :doc "Set to `true` to configure the value cache to not expire after data updates."}
     :viewport-datasource {:camel "viewportDatasource" :type :object :doc "To use the viewport row model you need to provide the grid with a `viewportDatasource`."}
     :viewport-row-model-buffer-size {:camel "viewportRowModelBufferSize" :type :number :initial? true :doc "When using viewport row model, sets the buffer size for the viewport."}
     :viewport-row-model-page-size {:camel "viewportRowModelPageSize" :type :number :initial? true :doc "When using viewport row model, sets the page size for the viewport."}}
     :col-def
     {:agg-func {:camel "aggFunc" :type :object :doc "Name of function to use for aggregation. In-built options are: `sum`, `min`, `max`, `count`, `avg`, `first`, `last`. Also accepts a custom aggregation name or an aggregation function."}
     :allow-formula {:camel "allowFormula" :type :boolean :default "false" :doc "Allow formulas to be entered and evaluated in this column."}
     :allowed-agg-funcs {:camel "allowedAggFuncs" :type :array :doc "Aggregation functions allowed on this column e.g. `['sum', 'avg']`."}
     :auto-header-height {:camel "autoHeaderHeight" :type :boolean :default "false" :doc "If enabled then the column header row will automatically adjust height to accommodate the size of the header cell."}
     :auto-height {:camel "autoHeight" :type :boolean :default "false" :doc "Set to `true` to have the grid calculate the height of a row based on contents of this column."}
     :calculated-expression {:camel "calculatedExpression" :type :string :doc "Expression used to calculate this column's value from other columns in the same row."}
     :cell-aria-role {:camel "cellAriaRole" :type :string :default "'gridcell'" :doc "Used for screen reader announcements - the role property of the cells that belong to this column."}
     :cell-class {:camel "cellClass" :type :object :doc "Class to use for the cell. Can be string, array of strings, or function that returns a string or array of strings."}
     :cell-class-rules {:camel "cellClassRules" :type :object :doc "Rules which can be applied to include certain CSS classes."}
     :cell-data-type {:camel "cellDataType" :type :object :default "true" :doc "The data type of the cell values for this column."}
     :cell-editor {:camel "cellEditor" :type :any :doc "Provide your own cell editor component for this column's cells."}
     :cell-editor-params {:camel "cellEditorParams" :type :any :doc "Params to be passed to the `cellEditor` component."}
     :cell-editor-popup {:camel "cellEditorPopup" :type :boolean :doc "Set to `true`, to have the cell editor appear in a popup."}
     :cell-editor-popup-position {:camel "cellEditorPopupPosition" :type :object :default "'over'" :doc "Set the position for the popup cell editor. Possible values are"}
     :cell-editor-selector {:camel "cellEditorSelector" :type :object :doc "Callback to select which cell editor to be used for a given row within the same column."}
     :cell-renderer {:camel "cellRenderer" :type :any :doc "Provide your own cell Renderer component for this column's cells."}
     :cell-renderer-params {:camel "cellRendererParams" :type :any :doc "Params to be passed to the `cellRenderer` component."}
     :cell-renderer-selector {:camel "cellRendererSelector" :type :object :doc "Callback to select which cell renderer to be used for a given row within the same column."}
     :cell-style {:camel "cellStyle" :type :object :doc "An object of CSS values / or function returning an object of CSS values for a particular cell."}
     :chart-data-type {:camel "chartDataType" :type :object :doc "Defines the chart data type that should be used for a column."}
     :checkbox-selection {:camel "checkboxSelection" :type :object :default "false" :deprecated "v32.2 Use the new selection API instead. See `GridOptions.rowSelection` Set to `true` (or return `true` from function) to render a selection checkbox in the column."}
     :col-id {:camel "colId" :type :string :doc "The unique ID to give the column. This is optional. If missing, the ID will default to the field."}
     :col-span {:camel "colSpan" :type :object :doc "By default, each cell will take up the width of one column. You can change this behaviour to allow cells to span multiple columns."}
     :column-chooser-params {:camel "columnChooserParams" :type :object :doc "Params used to change the behaviour and appearance of the Column Chooser/Columns Menu tab."}
     :column-group-show {:camel "columnGroupShow" :type :object :doc "Whether to only show the column when the group is open / closed. If not set the column is always displayed as part of the group."}
     :comparator {:camel "comparator" :type :object :doc "Override the default sorting order by providing a custom sort comparator, or a map of comparators for different `SortType`s."}
     :context {:camel "context" :type :any :doc "Context property that can be used to associate arbitrary application data with this column definition."}
     :context-menu-items {:camel "contextMenuItems" :type :object :doc "Customise the list of menu items available in the context menu."}
     :date-component {:camel "dateComponent" :type :any :doc "Custom date selection component to be used in Date Filters and Date Floating Filters for this column."}
     :date-component-params {:camel "dateComponentParams" :type :any :doc "The parameters to be passed to the `dateComponent`."}
     :default-agg-func {:camel "defaultAggFunc" :type :string :default "'sum'" :doc "The name of the aggregation function to use for this column when it is enabled via the GUI."}
     :dnd-source {:camel "dndSource" :type :object :default "false" :doc "`boolean` or `Function`. Set to `true` (or return `true` from function) to allow dragging for native drag and drop."}
     :dnd-source-on-row-drag {:camel "dndSourceOnRowDrag" :type :object :doc "Function to allow custom drag functionality for native drag and drop."}
     :editable {:camel "editable" :type :object :default "false" :doc "Set to `true` if this column is editable, otherwise `false`. Can also be a function to have different rows editable."}
     :enable-cell-change-flash {:camel "enableCellChangeFlash" :type :boolean :default "false" :doc "Set to `true` to flash a cell when it's refreshed."}
     :enable-pivot {:camel "enablePivot" :type :boolean :default "false" :doc "Set to `true` if you want to be able to pivot by this column via the GUI. This will not block the API or properties being used to achieve pivot."}
     :enable-row-group {:camel "enableRowGroup" :type :boolean :default "false" :doc "Set to `true` if you want to be able to row group by this column via the GUI."}
     :enable-show-values-as {:camel "enableShowValuesAs" :type :boolean :default "false" :doc "Shows the \"Show Values As\" submenu in the column menu."}
     :enable-value {:camel "enableValue" :type :boolean :default "false" :doc "Set to `true` if you want to be able to aggregate by this column via the GUI."}
     :equals {:camel "equals" :type :object :doc "Custom comparator for values, used by renderer to know if values have changed. Cells whose values have not changed don't get refreshed."}
     :field {:camel "field" :type :object :doc "The field of the row object to get the cell's data from."}
     :filter {:camel "filter" :type :any :doc "Filter to use for this column."}
     :filter-params {:camel "filterParams" :type :any :doc "Params to be passed to the filter component specified in `filter`."}
     :filter-value-getter {:camel "filterValueGetter" :type :object :doc "Function or expression. Gets the value for filtering purposes."}
     :flex {:camel "flex" :type :number :doc "Equivalent to `flex-grow` in CSS. When `flex` is set on one or more"}
     :floating-filter {:camel "floatingFilter" :type :boolean :default "false" :doc "Whether to display a floating filter for this column."}
     :floating-filter-component {:camel "floatingFilterComponent" :type :any :doc "The custom component to be used for rendering the floating filter."}
     :floating-filter-component-params {:camel "floatingFilterComponentParams" :type :any :doc "Params to be passed to `floatingFilterComponent`."}
     :get-find-text {:camel "getFindText" :type :object :doc "When using Find with custom cell renderers, this allows providing a custom value to search within."}
     :get-quick-filter-text {:camel "getQuickFilterText" :type :object :doc "A function to tell the grid what Quick Filter text to use for this column if you don't want to use the default (which is calling `toString` on the value)."}
     :group-hierarchy {:camel "groupHierarchy" :type :array :doc "Specify a grouping hierarchy for this column. This generates one or more virtual columns to group or pivot by when this column is grouped or pivoted."}
     :group-row-editable {:camel "groupRowEditable" :type :object :doc "Works like `editable`, but is evaluated only for group rows. When provided, group rows use"}
     :group-row-value-setter {:camel "groupRowValueSetter" :type :object :doc "Controls how a group row value edit is distributed to descendant rows."}
     :header-checkbox-selection {:camel "headerCheckboxSelection" :type :object :deprecated "v32.2 Use the new selection API instead. See `GridOptions.rowSelection` If `true` or the callback returns `true`, a 'select all' checkbox will be put into the header."}
     :header-checkbox-selection-current-page-only {:camel "headerCheckboxSelectionCurrentPageOnly" :type :boolean :default "false" :deprecated "v32.2 Use the new selection API instead. See `GridOptions.rowSelection` If `true`, the header checkbox selection will only select nodes on the current page."}
     :header-checkbox-selection-filtered-only {:camel "headerCheckboxSelectionFilteredOnly" :type :boolean :default "false" :deprecated "v32.2 Use the new selection API instead. See `GridOptions.rowSelection` If `true`, the header checkbox selection will only select filtered items."}
     :header-class {:camel "headerClass" :type :object :doc "CSS class to use for the header cell. Can be a string, array of strings, or function."}
     :header-component {:camel "headerComponent" :type :any :doc "The custom header component to be used for rendering the component header. If none specified the default AG Grid header component is used."}
     :header-component-params {:camel "headerComponentParams" :type :any :doc "The parameters to be passed to the `headerComponent`."}
     :header-name {:camel "headerName" :type :string :doc "The name to render in the column header. If not specified and field is specified, the field name will be used as the header name."}
     :header-style {:camel "headerStyle" :type :object :doc "An object of CSS values / or function returning an object of CSS values for a particular header."}
     :header-tooltip {:camel "headerTooltip" :type :string :doc "Tooltip for the column header, `headerTooltipValueGetter` takes precedence if set."}
     :header-tooltip-value-getter {:camel "headerTooltipValueGetter" :type :object :doc "Callback that should return the string to use for a tooltip."}
     :header-value-getter {:camel "headerValueGetter" :type :object :doc "Function or expression. Gets the value for display in the header."}
     :hide {:camel "hide" :type :boolean :default "false" :doc "Set to `true` for this column to be hidden."}
     :icons {:camel "icons" :type :object :initial? true :doc "Icons to use inside the column instead of the grid's default icons. Leave undefined to use defaults."}
     :initial-agg-func {:camel "initialAggFunc" :type :object :initial? true :doc "Same as `aggFunc`, except only applied when creating a new column. Not applied when updating column definitions."}
     :initial-flex {:camel "initialFlex" :type :number :initial? true :doc "Same as `flex`, except only applied when creating a new column. Not applied when updating column definitions."}
     :initial-hide {:camel "initialHide" :type :boolean :initial? true :doc "Same as `hide`, except only applied when creating a new column. Not applied when updating column definitions."}
     :initial-pinned {:camel "initialPinned" :type :object :initial? true :doc "Same as `pinned`, except only applied when creating a new column. Not applied when updating column definitions."}
     :initial-pivot {:camel "initialPivot" :type :boolean :initial? true :doc "Same as `pivot`, except only applied when creating a new column. Not applied when updating column definitions."}
     :initial-pivot-index {:camel "initialPivotIndex" :type :number :initial? true :doc "Same as `pivotIndex`, except only applied when creating a new column. Not applied when updating column definitions."}
     :initial-row-group {:camel "initialRowGroup" :type :boolean :initial? true :doc "Same as `rowGroup`, except only applied when creating a new column. Not applied when updating column definitions."}
     :initial-row-group-index {:camel "initialRowGroupIndex" :type :number :initial? true :doc "Same as `rowGroupIndex`, except only applied when creating a new column. Not applied when updating column definitions."}
     :initial-show-values-as {:camel "initialShowValuesAs" :type :object :initial? true :doc "Same as `showValuesAs`, except only applied when creating a new column."}
     :initial-sort {:camel "initialSort" :type :object :initial? true :doc "Same as `sort`, except only applied when creating a new column. Not applied when updating column definitions."}
     :initial-sort-index {:camel "initialSortIndex" :type :number :initial? true :doc "Same as `sortIndex`, except only applied when creating a new column. Not applied when updating column definitions."}
     :initial-value-index {:camel "initialValueIndex" :type :number :initial? true :doc "Same as `valueIndex`, except only applied when creating a new column. Not applied when updating column definitions."}
     :initial-width {:camel "initialWidth" :type :number :initial? true :doc "Same as `width`, except only applied when creating a new column. Not applied when updating column definitions."}
     :key-creator {:camel "keyCreator" :type :object :doc "Function to return a string key for a value."}
     :loading-cell-renderer {:camel "loadingCellRenderer" :type :any :doc "The renderer to be used while either"}
     :loading-cell-renderer-params {:camel "loadingCellRendererParams" :type :any :doc "Params to be passed to the `loadingCellRenderer` component."}
     :loading-cell-renderer-selector {:camel "loadingCellRendererSelector" :type :object :doc "Callback to select which loading renderer to be used."}
     :lock-pinned {:camel "lockPinned" :type :boolean :default "false" :doc "Set to true to block the user pinning the column, the column can only be pinned via definitions or API."}
     :lock-position {:camel "lockPosition" :type :object :doc "Lock a column to position to `'left'` or`'right'` to always have this column displayed in that position. `true` is treated as `'left'`"}
     :lock-visible {:camel "lockVisible" :type :boolean :default "false" :doc "Set to `true` to block making column visible / hidden via the UI (API will still work)."}
     :main-menu-items {:camel "mainMenuItems" :type :object :doc "Customise the list of menu items available in the column menu."}
     :max-width {:camel "maxWidth" :type :number :doc "Maximum width in pixels for the cell."}
     :menu-tabs {:camel "menuTabs" :type :array :doc "Set to an array containing zero, one or many of the following options: `'filterMenuTab' | 'generalMenuTab' | 'columnsMenuTab'`."}
     :min-width {:camel "minWidth" :type :number :doc "Minimum width in pixels for the cell."}
     :on-cell-clicked {:camel "onCellClicked" :type :function :doc "Callback called when a cell is clicked."}
     :on-cell-context-menu {:camel "onCellContextMenu" :type :function :doc "Callback called when a cell is right clicked."}
     :on-cell-double-clicked {:camel "onCellDoubleClicked" :type :function :doc "Callback called when a cell is double clicked."}
     :on-cell-value-changed {:camel "onCellValueChanged" :type :function :doc "Callback for after the value of a cell has changed, either due to editing or the application calling `api.setValue()`."}
     :pinned {:camel "pinned" :type :object :doc "Pin a column to one side: `right` or `left`. A value of `true` is converted to `'left'`."}
     :pivot {:camel "pivot" :type :boolean :doc "Set to true to pivot by this column."}
     :pivot-comparator {:camel "pivotComparator" :type :object :initial? true :doc "Only for CSRM, see [SSRM Pivoting](https://www.ag-grid.com/javascript-data-grid/server-side-model-pivoting/)."}
     :pivot-index {:camel "pivotIndex" :type :number :doc "Set this in columns you want to pivot by."}
     :pivot-keys {:camel "pivotKeys" :type :array :doc "Never set this, it is used internally by grid when doing in-grid pivoting"}
     :pivot-total-column-ids {:camel "pivotTotalColumnIds" :type :array :doc "Never set this, it is used internally by grid when doing in-grid pivoting"}
     :pivot-value-column {:camel "pivotValueColumn" :type :object :doc "Never set this, it is used internally by grid when doing in-grid pivoting"}
     :ref-data {:camel "refData" :type :object :doc "Provided a reference data map to be used to map column values to their respective value from the map."}
     :resizable {:camel "resizable" :type :boolean :default "true" :doc "Set to `false` to disable resizing which is enabled by default."}
     :row-drag {:camel "rowDrag" :type :object :default "false" :doc "`boolean` or `Function`. Set to `true` (or return `true` from function) to allow row dragging."}
     :row-drag-text {:camel "rowDragText" :type :object :doc "A callback that should return a string to be displayed by the `rowDragComp` while dragging a row."}
     :row-group {:camel "rowGroup" :type :boolean :default "false" :doc "Set to `true` to row group by this column."}
     :row-group-index {:camel "rowGroupIndex" :type :number :doc "Set this in columns you want to group by."}
     :row-grouping-hierarchy {:camel "rowGroupingHierarchy" :type :array :deprecated "deprecated" :doc "Specify a grouping hierarchy for this column. This generates one or more virtual columns to group or pivot by when this column is grouped or pivoted."}
     :row-span {:camel "rowSpan" :type :object :doc "By default, each cell will take up the height of one row. You can change this behaviour to allow cells to span multiple rows."}
     :show-disabled-checkboxes {:camel "showDisabledCheckboxes" :type :boolean :default "false" :deprecated "v32.2 Use the new selection API instead. See `GridOptions.rowSelection` Set to `true` to display a disabled checkbox when row is not selectable and checkboxes are enabled."}
     :show-row-group {:camel "showRowGroup" :type :object :initial? true :doc "Set to true to have the grid place the values for the group into the cell, or put the name of a grouped column to just show that group."}
     :show-values-as {:camel "showValuesAs" :type :object :doc "The active \"Show Values As\" mode for this column."}
     :show-values-as-def {:camel "showValuesAsDef" :type :object :doc "Per-column \"Show Values As\" configuration: `precision`, `suppressHeaderIndicator`, and user-provided"}
     :single-click-edit {:camel "singleClickEdit" :type :boolean :default "false" :doc "Set to `true` to have cells under this column enter edit mode after single click."}
     :sort {:camel "sort" :type :object :doc "Set the default sort."}
     :sort-index {:camel "sortIndex" :type :number :doc "If sorting more than one column by default, specifies order in which the sorting should be applied."}
     :sortable {:camel "sortable" :type :boolean :default "true" :doc "Set to `false` to disable sorting which is enabled by default."}
     :sorting-order {:camel "sortingOrder" :type :array :doc "An array defining the order in which sorting occurs (if sorting is enabled)."}
     :span-rows {:camel "spanRows" :type :object :doc "Set to `true` to automatically merge cells in this column with equal values. Provide a callback to specify custom merging logic."}
     :suppress-auto-size {:camel "suppressAutoSize" :type :boolean :default "false" :doc "Set to `true` if you do not want this column to be auto-resizable during 'size to contents' operations."}
     :suppress-columns-tool-panel {:camel "suppressColumnsToolPanel" :type :boolean :default "false" :doc "Set to `true` if you do not want this column or group to appear in the Columns Tool Panel."}
     :suppress-fill-handle {:camel "suppressFillHandle" :type :boolean :doc "Set to true to prevent the fillHandle from being rendered in any cell that belongs to this column"}
     :suppress-filters-tool-panel {:camel "suppressFiltersToolPanel" :type :boolean :default "false" :doc "Set to `true` if you do not want this column (filter) or group (filter group) to appear in the Filters Tool Panel."}
     :suppress-floating-filter-button {:camel "suppressFloatingFilterButton" :type :boolean :doc "If `true`, the button in the floating filter that opens the parent filter in a popup will not be displayed."}
     :suppress-header-context-menu {:camel "suppressHeaderContextMenu" :type :boolean :default "false" :doc "Set to `true` to not display the column menu when the column header is right-clicked."}
     :suppress-header-filter-button {:camel "suppressHeaderFilterButton" :type :boolean :default "false" :doc "Set to `true` to not display the filter button in the column header."}
     :suppress-header-keyboard-event {:camel "suppressHeaderKeyboardEvent" :type :object :doc "Suppress the grid taking action for the relevant keyboard event when a header is focused."}
     :suppress-header-menu-button {:camel "suppressHeaderMenuButton" :type :boolean :default "false" :doc "Set to `true` if no menu button should be shown for this column header."}
     :suppress-keyboard-event {:camel "suppressKeyboardEvent" :type :object :default "false" :doc "Allows the user to suppress certain keyboard events in the grid cell."}
     :suppress-movable {:camel "suppressMovable" :type :boolean :default "false" :doc "Set to `true` if you do not want this column to be movable via dragging."}
     :suppress-navigable {:camel "suppressNavigable" :type :object :default "false" :doc "Set to `true` if this column is not navigable (i.e. cannot be tabbed into), otherwise `false`."}
     :suppress-note-actions {:camel "suppressNoteActions" :type :object :default "false" :doc "Set to `true` to suppress built-in note actions for this column."}
     :suppress-paste {:camel "suppressPaste" :type :object :doc "Pasting is on by default as long as cells are editable (non-editable cells cannot be modified, even with a paste operation)."}
     :suppress-size-to-fit {:camel "suppressSizeToFit" :type :boolean :default "false" :doc "Set to `true` if you want this column's width to be fixed during 'size to fit' operations."}
     :suppress-span-header-height {:camel "suppressSpanHeaderHeight" :type :boolean :default "false" :doc "Set to `true` if you don't want the column header for this column to span the whole height of the header container."}
     :tool-panel-class {:camel "toolPanelClass" :type :object :doc "CSS class to use for the tool panel cell. Can be a string, array of strings, or function."}
     :tooltip-component {:camel "tooltipComponent" :type :any :doc "Provide your own tooltip component for the column."}
     :tooltip-component-params {:camel "tooltipComponentParams" :type :any :doc "The params used to configure `tooltipComponent`."}
     :tooltip-component-selector {:camel "tooltipComponentSelector" :type :object :doc "Callback to select which tooltip component to be used for a given row within the same column."}
     :tooltip-field {:camel "tooltipField" :type :object :doc "The field of the tooltip to apply to the cell."}
     :tooltip-value-getter {:camel "tooltipValueGetter" :type :object :doc "Callback that should return the string to use for a tooltip, `tooltipField` takes precedence if set."}
     :type {:camel "type" :type :array :doc "A comma separated string or array of strings containing `ColumnType` keys which can be used as a template for a column."}
     :un-sort-icon {:camel "unSortIcon" :type :boolean :default "false" :doc "Set to `true` if you want the unsorted icon to be shown when no sort is applied to this column."}
     :use-value-formatter-for-export {:camel "useValueFormatterForExport" :type :boolean :default "true" :doc "By default, values are formatted using the column's `valueFormatter` when exporting data from the grid."}
     :use-value-parser-for-import {:camel "useValueParserForImport" :type :boolean :default "true" :doc "By default, values are parsed using the column's `valueParser` when importing data to the grid."}
     :value-formatter {:camel "valueFormatter" :type :object :doc "A function or expression to format a value, should return a string."}
     :value-getter {:camel "valueGetter" :type :object :doc "Function or expression. Gets the value from your data for display."}
     :value-index {:camel "valueIndex" :type :number :doc "The position of this column in the order of value columns when aggregating in pivot mode."}
     :value-parser {:camel "valueParser" :type :object :doc "Function or expression. Parses the value for saving."}
     :value-setter {:camel "valueSetter" :type :object :doc "Function or expression. Sets the value into your data for saving. Return `true` if the data changed."}
     :width {:camel "width" :type :number :doc "Initial width in pixels for the cell."}
     :wrap-header-text {:camel "wrapHeaderText" :type :boolean :doc "If enabled then column header names that are too long for the column width will wrap onto the next line. Default `false`"}
     :wrap-text {:camel "wrapText" :type :boolean :default "false" :doc "Set to `true` to have the text wrap inside the cell - typically used with `autoHeight`."}}
     :col-group-def
     {:auto-header-height {:camel "autoHeaderHeight" :type :boolean :default "false" :doc "If enabled then the column header row will automatically adjust height to accommodate the size of the header cell."}
     :cell-aria-role {:camel "cellAriaRole" :type :string :default "'gridcell'" :doc "Used for screen reader announcements - the role property of the cells that belong to this column."}
     :children {:camel "children" :type :array :doc "A list containing a mix of columns and column groups."}
     :column-group-show {:camel "columnGroupShow" :type :object :doc "Whether to only show the column when the group is open / closed. If not set the column is always displayed as part of the group."}
     :context {:camel "context" :type :any :doc "Context property that can be used to associate arbitrary application data with this column definition."}
     :group-id {:camel "groupId" :type :string :doc "The unique ID to give the column. This is optional. If missing, a unique ID will be generated. This ID is used to identify the column group in the API."}
     :header-class {:camel "headerClass" :type :object :doc "CSS class to use for the header cell. Can be a string, array of strings, or function."}
     :header-group-component {:camel "headerGroupComponent" :type :any :doc "The custom header group component to be used for rendering the component header. If none specified the default AG Grid is used."}
     :header-group-component-params {:camel "headerGroupComponentParams" :type :any :doc "The params used to configure the `headerGroupComponent`."}
     :header-name {:camel "headerName" :type :string :doc "The name to render in the column header. If not specified and field is specified, the field name will be used as the header name."}
     :header-style {:camel "headerStyle" :type :object :doc "An object of CSS values / or function returning an object of CSS values for a particular header."}
     :header-tooltip {:camel "headerTooltip" :type :string :doc "Tooltip for the column header, `headerTooltipValueGetter` takes precedence if set."}
     :header-tooltip-value-getter {:camel "headerTooltipValueGetter" :type :object :doc "Callback that should return the string to use for a tooltip."}
     :header-value-getter {:camel "headerValueGetter" :type :object :doc "Function or expression. Gets the value for display in the header."}
     :main-menu-items {:camel "mainMenuItems" :type :object :doc "Customise the list of menu items available in the column group header context menu (on right-click)."}
     :marry-children {:camel "marryChildren" :type :boolean :default "false" :doc "Set to `true` to keep columns in this group beside each other in the grid. Moving the columns outside of the group (and hence breaking the group) is not allowed."}
     :open-by-default {:camel "openByDefault" :type :boolean :default "false" :doc "Set to `true` if this group should be opened by default."}
     :pivot-keys {:camel "pivotKeys" :type :array :doc "Never set this, it is used internally by grid when doing in-grid pivoting"}
     :suppress-columns-tool-panel {:camel "suppressColumnsToolPanel" :type :boolean :default "false" :doc "Set to `true` if you do not want this column or group to appear in the Columns Tool Panel."}
     :suppress-filters-tool-panel {:camel "suppressFiltersToolPanel" :type :boolean :default "false" :doc "Set to `true` if you do not want this column (filter) or group (filter group) to appear in the Filters Tool Panel."}
     :suppress-header-context-menu {:camel "suppressHeaderContextMenu" :type :boolean :default "false" :doc "Set to `true` to not display the column menu when the column header is right-clicked."}
     :suppress-header-keyboard-event {:camel "suppressHeaderKeyboardEvent" :type :object :doc "Suppress the grid taking action for the relevant keyboard event when a header is focused."}
     :suppress-sticky-label {:camel "suppressStickyLabel" :type :boolean :default "false" :doc "If `true` the label of the Column Group will not scroll alongside the grid to always remain visible."}
     :tool-panel-class {:camel "toolPanelClass" :type :object :doc "CSS class to use for the tool panel cell. Can be a string, array of strings, or function."}
     :tooltip-component {:camel "tooltipComponent" :type :any :doc "Provide your own tooltip component for the column."}
     :tooltip-component-params {:camel "tooltipComponentParams" :type :any :doc "The params used to configure `tooltipComponent`."}
     :wrap-header-text {:camel "wrapHeaderText" :type :boolean :doc "If enabled then column header names that are too long for the column width will wrap onto the next line. Default `false`"}}
     :events
     {:advanced-filter-builder-visible-changed {:event "advancedFilterBuilderVisibleChanged" :handler "onAdvancedFilterBuilderVisibleChanged"}
     :async-transactions-flushed {:event "asyncTransactionsFlushed" :handler "onAsyncTransactionsFlushed"}
     :batch-editing-started {:event "batchEditingStarted" :handler "onBatchEditingStarted"}
     :batch-editing-stopped {:event "batchEditingStopped" :handler "onBatchEditingStopped"}
     :body-scroll {:event "bodyScroll" :handler "onBodyScroll"}
     :body-scroll-end {:event "bodyScrollEnd" :handler "onBodyScrollEnd"}
     :bulk-editing-started {:event "bulkEditingStarted" :handler "onBulkEditingStarted"}
     :bulk-editing-stopped {:event "bulkEditingStopped" :handler "onBulkEditingStopped"}
     :calculated-column-created {:event "calculatedColumnCreated" :handler "onCalculatedColumnCreated"}
     :calculated-column-expression-changed {:event "calculatedColumnExpressionChanged" :handler "onCalculatedColumnExpressionChanged"}
     :calculated-column-removed {:event "calculatedColumnRemoved" :handler "onCalculatedColumnRemoved"}
     :calculated-column-validation-state-changed {:event "calculatedColumnValidationStateChanged" :handler "onCalculatedColumnValidationStateChanged"}
     :cell-clicked {:event "cellClicked" :handler "onCellClicked"}
     :cell-context-menu {:event "cellContextMenu" :handler "onCellContextMenu"}
     :cell-double-clicked {:event "cellDoubleClicked" :handler "onCellDoubleClicked"}
     :cell-edit-request {:event "cellEditRequest" :handler "onCellEditRequest"}
     :cell-editing-started {:event "cellEditingStarted" :handler "onCellEditingStarted"}
     :cell-editing-stopped {:event "cellEditingStopped" :handler "onCellEditingStopped"}
     :cell-focused {:event "cellFocused" :handler "onCellFocused"}
     :cell-key-down {:event "cellKeyDown" :handler "onCellKeyDown"}
     :cell-mouse-down {:event "cellMouseDown" :handler "onCellMouseDown"}
     :cell-mouse-out {:event "cellMouseOut" :handler "onCellMouseOut"}
     :cell-mouse-over {:event "cellMouseOver" :handler "onCellMouseOver"}
     :cell-selection-changed {:event "cellSelectionChanged" :handler "onCellSelectionChanged"}
     :cell-selection-delete-end {:event "cellSelectionDeleteEnd" :handler "onCellSelectionDeleteEnd"}
     :cell-selection-delete-start {:event "cellSelectionDeleteStart" :handler "onCellSelectionDeleteStart"}
     :cell-value-changed {:event "cellValueChanged" :handler "onCellValueChanged"}
     :chart-created {:event "chartCreated" :handler "onChartCreated"}
     :chart-destroyed {:event "chartDestroyed" :handler "onChartDestroyed"}
     :chart-options-changed {:event "chartOptionsChanged" :handler "onChartOptionsChanged"}
     :chart-range-selection-changed {:event "chartRangeSelectionChanged" :handler "onChartRangeSelectionChanged"}
     :column-everything-changed {:event "columnEverythingChanged" :handler "onColumnEverythingChanged"}
     :column-group-opened {:event "columnGroupOpened" :handler "onColumnGroupOpened"}
     :column-header-clicked {:event "columnHeaderClicked" :handler "onColumnHeaderClicked"}
     :column-header-context-menu {:event "columnHeaderContextMenu" :handler "onColumnHeaderContextMenu"}
     :column-header-mouse-leave {:event "columnHeaderMouseLeave" :handler "onColumnHeaderMouseLeave"}
     :column-header-mouse-over {:event "columnHeaderMouseOver" :handler "onColumnHeaderMouseOver"}
     :column-menu-visible-changed {:event "columnMenuVisibleChanged" :handler "onColumnMenuVisibleChanged"}
     :column-moved {:event "columnMoved" :handler "onColumnMoved"}
     :column-pinned {:event "columnPinned" :handler "onColumnPinned"}
     :column-pivot-changed {:event "columnPivotChanged" :handler "onColumnPivotChanged"}
     :column-pivot-mode-changed {:event "columnPivotModeChanged" :handler "onColumnPivotModeChanged"}
     :column-resized {:event "columnResized" :handler "onColumnResized"}
     :column-row-group-changed {:event "columnRowGroupChanged" :handler "onColumnRowGroupChanged"}
     :column-value-changed {:event "columnValueChanged" :handler "onColumnValueChanged"}
     :column-visible {:event "columnVisible" :handler "onColumnVisible"}
     :columns-reset {:event "columnsReset" :handler "onColumnsReset"}
     :component-state-changed {:event "componentStateChanged" :handler "onComponentStateChanged"}
     :context-menu-visible-changed {:event "contextMenuVisibleChanged" :handler "onContextMenuVisibleChanged"}
     :cut-end {:event "cutEnd" :handler "onCutEnd"}
     :cut-start {:event "cutStart" :handler "onCutStart"}
     :displayed-columns-changed {:event "displayedColumnsChanged" :handler "onDisplayedColumnsChanged"}
     :drag-cancelled {:event "dragCancelled" :handler "onDragCancelled"}
     :drag-started {:event "dragStarted" :handler "onDragStarted"}
     :drag-stopped {:event "dragStopped" :handler "onDragStopped"}
     :expand-or-collapse-all {:event "expandOrCollapseAll" :handler "onExpandOrCollapseAll"}
     :fill-end {:event "fillEnd" :handler "onFillEnd"}
     :fill-start {:event "fillStart" :handler "onFillStart"}
     :filter-changed {:event "filterChanged" :handler "onFilterChanged"}
     :filter-modified {:event "filterModified" :handler "onFilterModified"}
     :filter-opened {:event "filterOpened" :handler "onFilterOpened"}
     :filter-ui-changed {:event "filterUiChanged" :handler "onFilterUiChanged"}
     :find-changed {:event "findChanged" :handler "onFindChanged"}
     :first-data-rendered {:event "firstDataRendered" :handler "onFirstDataRendered"}
     :floating-filter-ui-changed {:event "floatingFilterUiChanged" :handler "onFloatingFilterUiChanged"}
     :grid-columns-changed {:event "gridColumnsChanged" :handler "onGridColumnsChanged"}
     :grid-pre-destroyed {:event "gridPreDestroyed" :handler "onGridPreDestroyed"}
     :grid-ready {:event "gridReady" :handler "onGridReady"}
     :grid-size-changed {:event "gridSizeChanged" :handler "onGridSizeChanged"}
     :header-focused {:event "headerFocused" :handler "onHeaderFocused"}
     :model-updated {:event "modelUpdated" :handler "onModelUpdated"}
     :new-columns-loaded {:event "newColumnsLoaded" :handler "onNewColumnsLoaded"}
     :pagination-changed {:event "paginationChanged" :handler "onPaginationChanged"}
     :paste-end {:event "pasteEnd" :handler "onPasteEnd"}
     :paste-start {:event "pasteStart" :handler "onPasteStart"}
     :pinned-row-data-changed {:event "pinnedRowDataChanged" :handler "onPinnedRowDataChanged"}
     :pinned-rows-changed {:event "pinnedRowsChanged" :handler "onPinnedRowsChanged"}
     :pivot-max-columns-exceeded {:event "pivotMaxColumnsExceeded" :handler "onPivotMaxColumnsExceeded"}
     :range-delete-end {:event "rangeDeleteEnd" :handler "onRangeDeleteEnd"}
     :range-delete-start {:event "rangeDeleteStart" :handler "onRangeDeleteStart"}
     :range-selection-changed {:event "rangeSelectionChanged" :handler "onRangeSelectionChanged"}
     :redo-ended {:event "redoEnded" :handler "onRedoEnded"}
     :redo-started {:event "redoStarted" :handler "onRedoStarted"}
     :row-clicked {:event "rowClicked" :handler "onRowClicked"}
     :row-data-updated {:event "rowDataUpdated" :handler "onRowDataUpdated"}
     :row-double-clicked {:event "rowDoubleClicked" :handler "onRowDoubleClicked"}
     :row-drag-cancel {:event "rowDragCancel" :handler "onRowDragCancel"}
     :row-drag-end {:event "rowDragEnd" :handler "onRowDragEnd"}
     :row-drag-enter {:event "rowDragEnter" :handler "onRowDragEnter"}
     :row-drag-leave {:event "rowDragLeave" :handler "onRowDragLeave"}
     :row-drag-move {:event "rowDragMove" :handler "onRowDragMove"}
     :row-editing-started {:event "rowEditingStarted" :handler "onRowEditingStarted"}
     :row-editing-stopped {:event "rowEditingStopped" :handler "onRowEditingStopped"}
     :row-group-opened {:event "rowGroupOpened" :handler "onRowGroupOpened"}
     :row-resize-ended {:event "rowResizeEnded" :handler "onRowResizeEnded"}
     :row-resize-started {:event "rowResizeStarted" :handler "onRowResizeStarted"}
     :row-selected {:event "rowSelected" :handler "onRowSelected"}
     :row-value-changed {:event "rowValueChanged" :handler "onRowValueChanged"}
     :selection-changed {:event "selectionChanged" :handler "onSelectionChanged"}
     :sort-changed {:event "sortChanged" :handler "onSortChanged"}
     :state-updated {:event "stateUpdated" :handler "onStateUpdated"}
     :store-refreshed {:event "storeRefreshed" :handler "onStoreRefreshed"}
     :tool-panel-size-changed {:event "toolPanelSizeChanged" :handler "onToolPanelSizeChanged"}
     :tool-panel-visible-changed {:event "toolPanelVisibleChanged" :handler "onToolPanelVisibleChanged"}
     :tooltip-hide {:event "tooltipHide" :handler "onTooltipHide"}
     :tooltip-show {:event "tooltipShow" :handler "onTooltipShow"}
     :undo-ended {:event "undoEnded" :handler "onUndoEnded"}
     :undo-started {:event "undoStarted" :handler "onUndoStarted"}
     :viewport-changed {:event "viewportChanged" :handler "onViewportChanged"}
     :virtual-columns-changed {:event "virtualColumnsChanged" :handler "onVirtualColumnsChanged"}
     :virtual-row-removed {:event "virtualRowRemoved" :handler "onVirtualRowRemoved"}}}))
