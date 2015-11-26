
/**
 * Test entry file
 *
 * This is programatically created and updated, do not modify
 *
 * context: {"env":"production","sourceMaps":false,"kbnVersion":"4.2.0","buildNum":8809}
 * includes code from:
 *  - elasticsearch@1.0.0
 *  - kbn_vislib_vis_types@1.0.0
 *  - kibana@1.0.0
 *  - markdown_vis@1.0.0
 *  - metric_vis@1.0.0
 *  - spyModes@1.0.0
 *  - statusPage@1.0.0
 *  - table_vis@1.0.0
 *
 */

require('ui/chrome')
require('plugins/kibana/kibana');
require('ui/directives/auto_select_if_only_one');
require('ui/directives/click_focus');
require('ui/directives/config');
require('ui/directives/confirm_click');
require('ui/directives/css_truncate');
require('ui/directives/field_name');
require('ui/directives/file_upload');
require('ui/directives/inequality');
require('ui/directives/infinite_scroll');
require('ui/directives/info');
require('ui/directives/input_datetime');
require('ui/directives/input_focus');
require('ui/directives/input_whole_number');
require('ui/directives/paginate');
require('ui/directives/pretty_duration');
require('ui/directives/rows');
require('ui/directives/saved_object_finder');
require('ui/directives/spinner');
require('ui/directives/truncated');
require('ui/directives/validate_cidr_mask');
require('ui/directives/validate_date_math');
require('ui/directives/validate_index_name');
require('ui/directives/validate_ip');
require('ui/directives/validate_json');
require('ui/filters/commaList');
require('ui/filters/field_type');
require('ui/filters/label');
require('ui/filters/match_any');
require('ui/filters/moment');
require('ui/filters/rison');
require('ui/filters/short_dots');
require('ui/filters/start_from');
require('ui/filters/trust_as_html');
require('ui/filters/unique');
require('ui/filters/uriescape');
require('ui/styles/theme.less');
require('ui/styles/base.less');
require('ui/styles/callout.less');
require('ui/styles/config.less');
require('ui/styles/control_group.less');
require('ui/styles/dark-theme.less');
require('ui/styles/dark-variables.less');
require('ui/styles/hintbox.less');
require('ui/styles/input.less');
require('ui/styles/list-group-menu.less');
require('ui/styles/navbar.less');
require('ui/styles/notify.less');
require('ui/styles/pagination.less');
require('ui/styles/sidebar.less');
require('ui/styles/spinner.less');
require('ui/styles/table.less');
require('ui/styles/theme');
require('ui/styles/truncate.less');
require('ui/styles/variables');
require('angular');
require('ui/chrome');
require('ui/chrome/context');
require('ui/bind');
require('ui/bound_to_config_obj');
require('ui/config');
require('ui/courier');
require('ui/debounce');
require('ui/doc_title');
require('ui/elastic_textarea');
require('ui/es');
require('ui/events');
require('ui/fancy_forms');
require('ui/filter_bar');
require('ui/filter_manager');
require('ui/index_patterns');
require('ui/listen');
require('ui/notify');
require('ui/persisted_log');
require('ui/private');
require('ui/promises');
require('ui/safe_confirm');
require('ui/state_management/app_state');
require('ui/state_management/global_state');
require('ui/storage');
require('ui/stringify/register');
require('ui/styleCompile');
require('ui/timefilter');
require('ui/timepicker');
require('ui/tooltip');
require('ui/typeahead');
require('ui/url');
require('ui/validateDateInterval');
require('ui/validate_query');
require('ui/watch_multi');
require('plugins/kibana/discover');
require('plugins/kibana/visualize');
require('plugins/kibana/dashboard');
require('plugins/kibana/settings');
require('plugins/kibana/settings/sections');
require('plugins/kibana/doc');
require('ui/vislib');
require('ui/agg_response');
require('ui/agg_types');
require('leaflet');
require('plugins/kbn_vislib_vis_types/kbn_vislib_vis_types');
require('plugins/markdown_vis/markdown_vis');
require('plugins/metric_vis/metric_vis');
require('plugins/table_vis/table_vis');
require('plugins/spyModes/tableSpyMode');
require('plugins/spyModes/reqRespStatsSpyMode');
require('ui/chrome').bootstrap(/* xoxo */);

