/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package im.xz.cn.web.view;

public class Css {

    public static String getBaseCss() {
        return """
            * { margin: 0; padding: 0; box-sizing: border-box; }

            body {
                font-family: var(--font-body);
                color: var(--color-text);
                min-height: 100vh;
                display: flex;
                flex-direction: column;
                background-color: var(--color-background);
                background-image:
                    radial-gradient(900px 520px at 12% -8%, rgba(255, 182, 193, 0.55), transparent 62%),
                    radial-gradient(820px 520px at 100% 0%, rgba(196, 181, 253, 0.42), transparent 58%),
                    radial-gradient(720px 620px at 50% 120%, rgba(255, 214, 232, 0.6), transparent 60%);
                background-attachment: fixed;
                -webkit-font-smoothing: antialiased;
                text-rendering: optimizeLegibility;
            }

            h1, h2, h3, h4, .brand-text, .card-title {
                font-family: var(--font-display);
                letter-spacing: 0.2px;
            }

            .navbar {
                position: sticky;
                top: 0;
                z-index: 100;
                background: rgba(255, 255, 255, 0.72);
                backdrop-filter: blur(18px) saturate(140%);
                -webkit-backdrop-filter: blur(18px) saturate(140%);
                border-bottom: 1px solid var(--color-border);
                box-shadow: var(--shadow-sm);
            }
            .navbar-inner {
                max-width: var(--content-max);
                margin: 0 auto;
                padding: 0 var(--space-6);
                display: flex;
                align-items: center;
                justify-content: space-between;
                height: var(--topbar-height);
                gap: var(--space-4);
            }
            .navbar-brand { display: flex; align-items: center; gap: var(--space-3); }
            .brand-icon {
                font-size: 22px;
                width: 38px;
                height: 38px;
                display: inline-flex;
                align-items: center;
                justify-content: center;
                border-radius: var(--radius-md);
                color: #fff;
                background: linear-gradient(135deg, var(--color-primary), var(--color-accent-purple));
                box-shadow: var(--shadow-glow);
            }
            .brand-text { font-size: var(--text-lg); font-weight: 700; color: var(--color-text); }
            .navbar-links { display: flex; align-items: center; gap: var(--space-2); flex-wrap: wrap; }
            .nav-link {
                padding: 9px 16px;
                border-radius: var(--radius-pill);
                text-decoration: none;
                color: var(--color-text-muted);
                font-size: var(--text-base);
                font-weight: 600;
                transition: all var(--duration-fast) var(--ease-standard);
            }
            .nav-link:hover { background: var(--color-surface-3); color: var(--color-primary-strong); }
            .nav-link.active {
                background: linear-gradient(135deg, var(--color-primary), var(--color-accent-purple));
                color: #fff;
                box-shadow: var(--shadow-glow);
            }

            .sidebar {
                width: var(--sidebar-width);
                background: var(--color-surface);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-xl);
                padding: var(--space-3);
                box-shadow: var(--shadow-sm);
                height: fit-content;
                position: sticky;
                top: calc(var(--topbar-height) + var(--space-6));
            }
            .sidebar-header {
                padding: var(--space-3) var(--space-4);
                font-family: var(--font-display);
                font-weight: 700;
                color: var(--color-primary-strong);
                display: flex;
                align-items: center;
                gap: var(--space-2);
                margin-bottom: var(--space-2);
            }
            .sidebar-item {
                display: flex;
                align-items: center;
                gap: var(--space-2);
                padding: 11px 14px;
                border-radius: var(--radius-md);
                text-decoration: none;
                color: var(--color-text-muted);
                font-size: var(--text-base);
                font-weight: 600;
                transition: all var(--duration-fast) var(--ease-standard);
                margin-bottom: 3px;
            }
            .sidebar-item:hover {
                background: var(--color-surface-3);
                color: var(--color-primary-strong);
                transform: translateX(3px);
            }
            .sidebar-item.active {
                background: linear-gradient(135deg, var(--color-primary), var(--color-accent-purple));
                color: #fff;
                box-shadow: var(--shadow-glow);
            }
            .sidebar-item i { width: 18px; text-align: center; }

            .card {
                background: var(--color-surface);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-xl);
                box-shadow: var(--shadow-sm);
                overflow: hidden;
                animation: fadeIn var(--duration-slow) var(--ease-emphasized);
                transition: transform var(--duration-normal) var(--ease-standard),
                            box-shadow var(--duration-normal) var(--ease-standard);
            }
            .card:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }
            .card-header {
                padding: var(--space-5) var(--space-6);
                border-bottom: 1px solid var(--color-border);
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: var(--space-3);
            }
            .card-title { font-size: var(--text-md); font-weight: 700; color: var(--color-text); }
            .card-body { padding: var(--space-6); }

            .section-title {
                font-family: var(--font-display);
                font-size: var(--text-xl);
                font-weight: 700;
                color: var(--color-text);
                margin-bottom: var(--space-5);
                display: flex;
                align-items: center;
                gap: var(--space-3);
            }

            .page-header { margin-bottom: var(--space-6); animation: fadeIn var(--duration-normal) var(--ease-standard); }
            .page-header h2 {
                font-family: var(--font-display);
                font-size: var(--text-xl);
                font-weight: 700;
                color: var(--color-text);
                margin-bottom: 4px;
            }
            .page-desc { font-size: var(--text-base); color: var(--color-text-muted); }

            .card-grid {
                display: grid;
                grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
                gap: var(--space-5);
            }
            .dashboard-grid { grid-template-columns: 1fr; }
            .stat-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                gap: var(--space-5);
            }

            .btn {
                padding: 11px 22px;
                border-radius: var(--radius-md);
                border: none;
                font-size: var(--text-base);
                font-weight: 600;
                font-family: var(--font-body);
                cursor: pointer;
                transition: all var(--duration-fast) var(--ease-standard);
                display: inline-flex;
                align-items: center;
                justify-content: center;
                gap: var(--space-2);
                text-decoration: none;
            }
            .btn:disabled { opacity: 0.55; cursor: not-allowed; }
            .btn-primary {
                background: linear-gradient(135deg, var(--color-primary), var(--color-accent-purple));
                color: #fff;
                box-shadow: var(--shadow-glow);
            }
            .btn-primary:hover:not(:disabled) { transform: translateY(-2px); filter: brightness(1.04); }
            .btn-secondary {
                background: var(--color-surface);
                color: var(--color-primary-strong);
                border: 1px solid var(--color-border-strong);
            }
            .btn-secondary:hover:not(:disabled) { background: var(--color-surface-3); }
            .btn-danger {
                background: linear-gradient(135deg, var(--color-danger), #DC2626);
                color: #fff;
                box-shadow: 0 8px 22px rgba(239, 68, 68, 0.3);
            }
            .btn-danger:hover:not(:disabled) { transform: translateY(-2px); }

            .form-input {
                width: 100%;
                padding: 12px 16px;
                border-radius: var(--radius-md);
                border: 1.5px solid var(--color-border-strong);
                font-size: var(--text-base);
                font-family: var(--font-body);
                color: var(--color-text);
                background: var(--color-surface);
                transition: all var(--duration-fast) var(--ease-standard);
                outline: none;
            }
            .form-input::placeholder { color: var(--color-text-faint); }
            input[type="number"]::-webkit-outer-spin-button,
            input[type="number"]::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
            input[type="number"] { -moz-appearance: textfield; appearance: textfield; }
            .form-input:focus {
                border-color: var(--color-primary);
                box-shadow: 0 0 0 4px rgba(255, 105, 180, 0.14);
            }
            .form-label {
                display: block;
                font-size: var(--text-base);
                font-weight: 600;
                color: var(--color-text);
                margin-bottom: 6px;
            }
            .form-group { margin-bottom: var(--space-4); }

            .table-wrap {
                background: var(--color-surface);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-xl);
                overflow: hidden;
                box-shadow: var(--shadow-sm);
            }
            .table { width: 100%; border-collapse: collapse; }
            .table th {
                background: var(--color-surface-3);
                padding: 14px 16px;
                text-align: left;
                font-size: var(--text-xs);
                font-weight: 700;
                text-transform: uppercase;
                letter-spacing: 0.4px;
                color: var(--color-primary-deep);
                border-bottom: 1px solid var(--color-border);
            }
            .table td {
                padding: 14px 16px;
                font-size: var(--text-base);
                border-bottom: 1px solid var(--color-border);
            }
            .table tbody tr:last-child td { border-bottom: none; }
            .table tbody tr:hover { background: var(--color-surface-2); }

            .alert {
                padding: 14px 18px;
                border-radius: var(--radius-md);
                margin-bottom: var(--space-4);
                display: flex;
                align-items: center;
                gap: var(--space-2);
                font-size: var(--text-base);
                border-left: 4px solid transparent;
                animation: slideDown var(--duration-normal) var(--ease-emphasized);
            }
            .alert-success { background: #ECFDF5; color: #047857; border-left-color: var(--color-success); }
            .alert-error { background: #FEF2F2; color: #B91C1C; border-left-color: var(--color-danger); }
            .alert-warning { background: #FFFBEB; color: #B45309; border-left-color: var(--color-warning); }
            .alert-info { background: #EFF6FF; color: #1D4ED8; border-left-color: var(--color-info); }

            .container {
                width: 100%;
                max-width: var(--content-max);
                margin: 0 auto;
                padding: var(--space-6);
            }
            .admin-layout, .user-layout {
                display: flex;
                gap: var(--space-6);
                width: 100%;
                max-width: var(--content-max);
                margin: var(--space-6) auto;
                padding: 0 var(--space-6);
                align-items: flex-start;
            }
            .admin-content, .user-content { flex: 1; min-width: 0; }

            .page-footer {
                text-align: center;
                padding: var(--space-8) var(--space-4);
                color: var(--color-text-faint);
                font-size: var(--text-sm);
                margin-top: auto;
                border-top: 1px solid var(--color-border);
            }
            .page-footer p { margin: 0; }

            .badge {
                display: inline-flex;
                align-items: center;
                gap: 4px;
                padding: 3px 11px;
                border-radius: var(--radius-pill);
                font-size: var(--text-xs);
                font-weight: 700;
            }
            .badge-success { background: #ECFDF5; color: #047857; }
            .badge-warning { background: #FFFBEB; color: #B45309; }
            .badge-danger { background: #FEF2F2; color: #B91C1C; }
            .badge-info { background: #EFF6FF; color: #1D4ED8; }

            .empty-hint { color: var(--color-text-faint); text-align: center; padding: var(--space-10) var(--space-4); }
            .empty-hint a { color: var(--color-primary-strong); }
            .skeleton {
                background: linear-gradient(90deg, var(--color-surface-3) 25%, #fff 37%, var(--color-surface-3) 63%);
                background-size: 400% 100%;
                animation: skeleton 1.4s ease infinite;
                border-radius: var(--radius-md);
            }

            .sortable-item { cursor: grab; }
            .sortable-item:active { cursor: grabbing; }
            .sortable-item.dragging { opacity: 0.55; transform: scale(0.99); }
            .sortable-item.dragging * { pointer-events: none; }
            .layout-reset-bar {
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: var(--space-3);
                margin-bottom: var(--space-3);
                flex-wrap: wrap;
            }
            .layout-hint {
                font-size: var(--text-xs);
                color: var(--color-text-faint);
                display: inline-flex;
                align-items: center;
                gap: 6px;
            }
            .layout-reset-btn {
                background: var(--color-surface);
                border: 1px solid var(--color-border-strong);
                color: var(--color-text-muted);
                border-radius: var(--radius-pill);
                padding: 7px 15px;
                font-size: var(--text-xs);
                font-weight: 600;
                font-family: var(--font-body);
                cursor: pointer;
                transition: all var(--duration-fast) var(--ease-standard);
                display: inline-flex;
                align-items: center;
                gap: 6px;
            }
            .layout-reset-btn:hover { color: var(--color-primary-strong); border-color: var(--color-primary); }

            .theme-toggle, .lang-toggle {
                background: none;
                border: 1px solid var(--color-border-strong);
                cursor: pointer;
                font-family: var(--font-body);
            }
            .lang-dropdown { position: relative; display: inline-flex; }
            .lang-menu {
                position: absolute;
                top: calc(100% + 8px);
                right: 0;
                min-width: 140px;
                background: var(--color-surface);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-lg);
                box-shadow: var(--shadow-lg);
                padding: 6px;
                display: none;
                flex-direction: column;
                gap: 2px;
                z-index: 300;
            }
            .lang-dropdown.open .lang-menu { display: flex; }
            .lang-item {
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 8px;
                border: none;
                background: transparent;
                color: var(--color-text);
                text-align: left;
                padding: 8px 12px;
                border-radius: var(--radius-md);
                font-size: var(--text-base);
                cursor: pointer;
                white-space: nowrap;
                transition: background var(--duration-fast) var(--ease-standard);
            }
            .lang-item:hover { background: var(--color-surface-3); }
            .lang-item.active { color: var(--color-primary-strong); font-weight: 600; }
            .lang-item i { font-size: 11px; }
            .lang-item-main { display: inline-flex; align-items: center; gap: 8px; }
            .lang-icon { width: 18px; height: 18px; border-radius: 5px; flex-shrink: 0; display: block; }
            .admin-sidebar .lang-dropdown { display: block; }
            .admin-sidebar .lang-menu {
                top: auto;
                bottom: calc(100% + 8px);
                left: 0;
                right: 0;
                min-width: 0;
            }

            :focus-visible { outline: 2px solid var(--color-primary); outline-offset: 2px; }
            ::selection { background: rgba(255, 105, 180, 0.28); }

            @keyframes fadeIn {
                from { opacity: 0; transform: translateY(10px); }
                to { opacity: 1; transform: translateY(0); }
            }
            @keyframes slideDown {
                from { opacity: 0; transform: translateY(-10px); }
                to { opacity: 1; transform: translateY(0); }
            }
            @keyframes skeleton {
                0% { background-position: 100% 50%; }
                100% { background-position: 0 50%; }
            }

            .menu-toggle {
                display: none;
                align-items: center;
                justify-content: center;
                width: 40px;
                height: 40px;
                border-radius: var(--radius-md);
                border: 1px solid var(--color-border-strong);
                background: var(--color-surface);
                color: var(--color-primary-strong);
                cursor: pointer;
                font-size: 16px;
            }
            .sidebar-backdrop {
                display: none;
                position: fixed;
                inset: 0;
                background: rgba(43, 35, 51, 0.5);
                backdrop-filter: blur(2px);
                z-index: 1100;
            }
            body.sidebar-open .sidebar-backdrop { display: block; }

            @media (max-width: 768px) {
                .navbar-inner { padding: 0 var(--space-4); }
                .menu-toggle { display: inline-flex; }
                .admin-layout, .user-layout { flex-direction: column; padding: 0 var(--space-4); }
                .admin-content, .user-content { overflow-x: auto; }
                .layout-reset-bar { display: none; }
                .sidebar {
                    display: block;
                    position: fixed;
                    top: 0;
                    left: 0;
                    bottom: 0;
                    width: 84vw;
                    max-width: 320px;
                    height: 100vh;
                    border-radius: 0 var(--radius-xl) var(--radius-xl) 0;
                    transform: translateX(-105%);
                    transition: transform var(--duration-normal) var(--ease-emphasized);
                    z-index: 1200;
                    overflow-y: auto;
                }
                body.sidebar-open .sidebar { transform: translateX(0); }
            }
            """;
    }

    public static String getAuthPageCss() {
        return """
            body { display: flex; flex-direction: column; min-height: 100vh; }
            .page-footer { margin-top: auto; }
            .auth-container {
                flex: 1;
                display: flex;
                justify-content: center;
                align-items: center;
                padding: var(--space-10) var(--space-6);
            }
            .auth-card {
                background: rgba(255, 255, 255, 0.86);
                backdrop-filter: blur(18px);
                -webkit-backdrop-filter: blur(18px);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-xl);
                padding: 46px 42px;
                box-shadow: var(--shadow-lg);
                width: 100%;
                max-width: 460px;
                animation: fadeIn var(--duration-slow) var(--ease-emphasized);
            }
            .auth-header { text-align: center; margin-bottom: var(--space-8); }
            .auth-icon {
                font-size: 40px;
                display: inline-flex;
                align-items: center;
                justify-content: center;
                width: 66px;
                height: 66px;
                border-radius: var(--radius-lg);
                color: #fff;
                background: linear-gradient(135deg, var(--color-primary), var(--color-accent-purple));
                box-shadow: var(--shadow-glow);
                margin-bottom: var(--space-4);
            }
            .auth-header h2 { color: var(--color-text); margin-bottom: 6px; font-size: var(--text-xl); }
            .auth-header .text-muted { font-size: var(--text-base); }
            .auth-footer { text-align: center; margin-top: var(--space-6); font-size: var(--text-base); color: var(--color-text-muted); }
            .auth-footer a { color: var(--color-primary-strong); text-decoration: none; font-weight: 600; }
            .auth-footer a:hover { text-decoration: underline; }
            .form-group { margin-bottom: var(--space-5); }
            .form-input { padding: 13px 16px; font-size: 15px; }
            .form-label { font-size: var(--text-base); margin-bottom: 8px; }
            .btn-block {
                width: 100%;
                justify-content: center;
                padding: 14px;
                font-size: var(--text-md);
                margin-top: var(--space-2);
                border-radius: var(--radius-md);
            }
            .required { color: var(--color-primary); }
            .form-row { display: flex; gap: var(--space-3); align-items: center; }
            .msg-area { margin-top: 6px; font-size: var(--text-sm); min-height: 18px; }
            .text-muted { color: var(--color-text-muted); }
            .form-hint { font-size: var(--text-xs); color: var(--color-primary-strong); margin-top: 6px; line-height: 1.7; }
            .form-hint span {
                display: inline-block;
                background: var(--color-surface-3);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-sm);
                padding: 1px 6px;
                margin: 2px 2px;
                font-family: var(--font-mono);
                font-size: var(--text-xs);
                color: var(--color-primary-strong);
            }
            @media (max-width: 520px) {
                .auth-card { padding: 34px 24px; }
            }
            """;
    }

    public static String getUserDashboardCss() {
        return """
            .welcome-section { margin-bottom: var(--space-6); }
            .welcome-section h2 {
                font-family: var(--font-display);
                color: var(--color-text);
                margin-bottom: 6px;
                font-size: var(--text-xl);
            }
            .text-muted { color: var(--color-text-muted); font-size: var(--text-base); margin: 4px 0; }
            .badge { padding: 3px 11px; border-radius: var(--radius-pill); font-size: var(--text-xs); font-weight: 700; }
            .badge-success { background: #ECFDF5; color: #047857; }
            .badge-warning { background: #FFFBEB; color: #B45309; }
            .yggdrasil-guide { padding: var(--space-1) 0; }
            .guide-step {
                display: flex;
                gap: var(--space-4);
                align-items: flex-start;
                padding: var(--space-4);
                background: var(--color-surface-2);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-lg);
                margin-bottom: var(--space-3);
                transition: all var(--duration-normal) var(--ease-standard);
            }
            .guide-step:hover { transform: translateX(4px); box-shadow: var(--shadow-sm); }
            .guide-step-num {
                flex-shrink: 0;
                width: 34px;
                height: 34px;
                border-radius: var(--radius-md);
                background: linear-gradient(135deg, var(--color-primary), var(--color-accent-purple));
                color: #fff;
                display: flex;
                align-items: center;
                justify-content: center;
                font-weight: 700;
                font-size: 15px;
                box-shadow: var(--shadow-glow);
            }
            .guide-step-body { flex: 1; min-width: 0; }
            .guide-step-title { font-weight: 700; font-size: var(--text-md); color: var(--color-text); margin-bottom: 4px; }
            .guide-step-desc { font-size: var(--text-base); color: var(--color-text-muted); line-height: 1.7; }
            .guide-step-desc a { color: var(--color-primary-strong); text-decoration: none; font-weight: 600; }
            .guide-step-desc a:hover { text-decoration: underline; }
            .guide-code {
                margin-top: var(--space-2);
                padding: 12px 14px;
                background: #F7F3FF;
                border: 1px solid #E9E1FB;
                border-radius: var(--radius-md);
                font-family: var(--font-mono);
                font-size: var(--text-sm);
                color: #5B21B6;
                word-break: break-all;
                user-select: all;
            }
            .guide-tips {
                margin-top: var(--space-4);
                padding: var(--space-4);
                background: #F0FDF4;
                border-radius: var(--radius-lg);
                border: 1px solid #BBF7D0;
            }
            .guide-tips-title { font-weight: 700; font-size: var(--text-base); color: #047857; margin-bottom: var(--space-2); }
            .guide-tips ul { margin: 0; padding-left: 20px; }
            .guide-tips li { font-size: var(--text-sm); color: var(--color-text-muted); line-height: 1.9; }
            .form-row { display: flex; gap: var(--space-3); align-items: center; }
            .inline-form { display: flex; gap: var(--space-3); align-items: center; }
            .msg-area { margin-top: var(--space-3); font-size: var(--text-base); min-height: 20px; }
            .msg-area.success { color: #047857; }
            .msg-area.error { color: #B91C1C; }
            .empty-hint { color: var(--color-text-faint); text-align: center; padding: var(--space-8) var(--space-4); }
            .empty-hint a { color: var(--color-primary-strong); }
            .modal-overlay {
                position: fixed;
                inset: 0;
                background: rgba(43, 35, 51, 0.45);
                backdrop-filter: blur(3px);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 200;
            }
            .modal-box {
                background: var(--color-surface);
                border-radius: var(--radius-xl);
                padding: var(--space-8);
                min-width: 360px;
                box-shadow: var(--shadow-lg);
                animation: fadeIn var(--duration-normal) var(--ease-emphasized);
            }
            .modal-box h3 { color: var(--color-text); margin-bottom: var(--space-4); font-family: var(--font-display); }
            .modal-actions { display: flex; gap: var(--space-3); justify-content: flex-end; margin-top: var(--space-5); }
            .btn-danger { background: linear-gradient(135deg, var(--color-danger), #DC2626); color: #fff; border: none; }
            .btn-danger:hover { filter: brightness(1.05); }
            .toast {
                position: fixed;
                bottom: var(--space-8);
                right: var(--space-8);
                padding: 14px 22px;
                border-radius: var(--radius-lg);
                font-size: var(--text-base);
                font-weight: 600;
                z-index: 9999;
                box-shadow: var(--shadow-lg);
                animation: toastSlideIn var(--duration-normal) var(--ease-emphasized);
            }
            .toast.toast-success { background: #ECFDF5; color: #047857; border-left: 4px solid var(--color-success); }
            .toast.toast-error { background: #FEF2F2; color: #B91C1C; border-left: 4px solid var(--color-danger); }
            .toast.toast-info { background: #EFF6FF; color: #1D4ED8; border-left: 4px solid var(--color-info); }
            @keyframes toastSlideIn {
                from { opacity: 0; transform: translateX(40px); }
                to { opacity: 1; transform: translateX(0); }
            }
            @media (max-width: 768px) {
                .sidebar { display: none; }
                .admin-layout, .user-layout { flex-direction: column; }
                .form-row, .inline-form { flex-direction: column; align-items: stretch; }
            }
            """;
    }

    public static String getTextureCss() {
        return """
            .welcome-section { margin-bottom: var(--space-6); }
            .welcome-section h2 { font-family: var(--font-display); color: var(--color-text); margin-bottom: 6px; }
            .text-muted { color: var(--color-text-muted); font-size: var(--text-base); margin: 4px 0; }
            .badge { padding: 3px 11px; border-radius: var(--radius-pill); font-size: var(--text-xs); font-weight: 700; }
            .badge-success { background: #ECFDF5; color: #047857; }
            .badge-warning { background: #FFFBEB; color: #B45309; }
            .profile-card {
                display: flex;
                align-items: center;
                gap: var(--space-4);
                padding: var(--space-4);
                background: var(--color-surface-2);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-lg);
                margin-bottom: var(--space-3);
                transition: all var(--duration-normal) var(--ease-standard);
            }
            .profile-card:hover { transform: translateX(4px); box-shadow: var(--shadow-sm); }
            .profile-avatar { font-size: 30px; color: var(--color-primary); }
            .profile-name { font-weight: 700; font-size: var(--text-md); color: var(--color-text); }
            .profile-uuid { font-size: var(--text-xs); color: var(--color-text-faint); font-family: var(--font-mono); }
            .profile-model { font-size: var(--text-xs); color: var(--color-text-muted); }
            .form-row { display: flex; gap: var(--space-3); align-items: center; }
            .inline-form { display: flex; gap: var(--space-3); align-items: center; }
            .msg-area { margin-top: var(--space-3); font-size: var(--text-base); min-height: 20px; }
            .msg-area.success { color: #047857; }
            .msg-area.error { color: #B91C1C; }
            .empty-hint { color: var(--color-text-faint); text-align: center; padding: var(--space-8) var(--space-4); }
            .empty-hint a { color: var(--color-primary-strong); }
            .modal-overlay {
                position: fixed;
                inset: 0;
                background: rgba(43, 35, 51, 0.45);
                backdrop-filter: blur(3px);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 200;
            }
            .modal-box {
                background: var(--color-surface);
                border-radius: var(--radius-xl);
                padding: var(--space-8);
                min-width: 360px;
                box-shadow: var(--shadow-lg);
                animation: fadeIn var(--duration-normal) var(--ease-emphasized);
            }
            .modal-box h3 { color: var(--color-text); margin-bottom: var(--space-4); font-family: var(--font-display); }
            .modal-actions { display: flex; gap: var(--space-3); justify-content: flex-end; margin-top: var(--space-5); }
            .btn-danger { background: linear-gradient(135deg, var(--color-danger), #DC2626); color: #fff; border: none; }
            .btn-danger:hover { filter: brightness(1.05); }
            @media (max-width: 768px) {
                .sidebar { display: none; }
                .admin-layout, .user-layout { flex-direction: column; }
                .form-row, .inline-form { flex-direction: column; align-items: stretch; }
            }
            """;
    }

    public static String getHomeCss() {
        return """
            .main-container {
                position: relative;
                z-index: 2;
                width: 100%;
                max-width: var(--content-max);
                margin: 0 auto;
                padding: var(--space-8) var(--space-6) var(--space-12);
                display: flex;
                flex-direction: column;
                gap: var(--space-10);
            }
            .petal-bg { position: fixed; inset: 0; pointer-events: none; z-index: 0; overflow: hidden; }
            .petal {
                position: absolute;
                background: radial-gradient(circle at 30% 30%, #ffb7c5, #ff8da1);
                border-radius: 80% 20% 70% 30% / 60% 50% 50% 40%;
                opacity: 0.22;
                animation: floatPetal 15s infinite ease-in-out;
                filter: blur(0.5px);
                box-shadow: 0 0 15px rgba(255, 140, 170, 0.4);
            }
            @keyframes floatPetal {
                0% { transform: translate(0, 0) rotate(0deg) scale(0.8); }
                25% { transform: translate(30px, -40px) rotate(90deg) scale(1.1); }
                50% { transform: translate(-20px, -80px) rotate(180deg) scale(0.9); }
                75% { transform: translate(15px, -120px) rotate(270deg) scale(1.2); }
                100% { transform: translate(0px, -160px) rotate(360deg) scale(0.7); }
            }
            .hero {
                display: flex;
                flex-direction: column;
                align-items: center;
                text-align: center;
                gap: var(--space-5);
                background: rgba(255, 255, 255, 0.66);
                backdrop-filter: blur(16px);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-xl);
                padding: var(--space-12) var(--space-8);
                box-shadow: var(--shadow-md);
            }
            .hero h1 {
                font-family: var(--font-display);
                font-size: var(--text-3xl);
                font-weight: 700;
                background: linear-gradient(135deg, var(--color-primary-strong), var(--color-accent-purple));
                -webkit-background-clip: text;
                background-clip: text;
                -webkit-text-fill-color: transparent;
                letter-spacing: 1px;
            }
            .hero .subtitle {
                font-size: var(--text-lg);
                color: var(--color-primary-strong);
                background: var(--color-surface-3);
                border: 1px solid var(--color-border);
                padding: 8px 22px;
                border-radius: var(--radius-pill);
                font-weight: 600;
            }
            .hero p { max-width: 620px; font-size: var(--text-md); color: var(--color-text-muted); }
            .hero-actions { display: flex; gap: var(--space-3); flex-wrap: wrap; justify-content: center; }
            .section-title {
                font-family: var(--font-display);
                font-size: var(--text-xl);
                font-weight: 700;
                color: var(--color-text);
                display: flex;
                align-items: center;
                gap: var(--space-3);
                justify-content: center;
                margin-bottom: var(--space-6);
            }
            .features-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: var(--space-5); }
            .feature-card {
                background: var(--color-surface);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-xl);
                padding: var(--space-8) var(--space-6);
                box-shadow: var(--shadow-sm);
                transition: all var(--duration-normal) var(--ease-standard);
                text-align: center;
                display: flex;
                flex-direction: column;
                gap: var(--space-3);
            }
            .feature-card:hover { transform: translateY(-6px); box-shadow: var(--shadow-md); border-color: var(--color-primary-light); }
            .icon-circle {
                width: 68px;
                height: 68px;
                border-radius: var(--radius-lg);
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 0 auto var(--space-2);
                font-size: 28px;
                color: #fff;
                background: linear-gradient(135deg, var(--color-primary), var(--color-accent-purple));
                box-shadow: var(--shadow-glow);
            }
            .feature-card h3 { font-family: var(--font-display); font-size: var(--text-lg); color: var(--color-text); }
            .feature-card p { color: var(--color-text-muted); }
            .highlight-badge {
                background: var(--color-surface-3);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-pill);
                padding: 5px 14px;
                font-size: var(--text-xs);
                font-weight: 700;
                color: var(--color-primary-strong);
                display: inline-block;
                margin-top: 4px;
            }
            .highlight-panel {
                display: flex;
                flex-wrap: wrap;
                gap: var(--space-6);
                align-items: center;
                background: rgba(255, 255, 255, 0.7);
                backdrop-filter: blur(14px);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-xl);
                padding: var(--space-8);
                box-shadow: var(--shadow-sm);
            }
            .highlight-intro { flex: 1; min-width: 220px; }
            .highlight-intro i { font-size: 40px; color: var(--color-primary); }
            .highlight-intro h2 { font-family: var(--font-display); color: var(--color-text); margin: 8px 0; }
            .highlight-intro h3 { color: var(--color-text-muted); font-size: var(--text-md); font-weight: 600; }
            .highlight-points { display: flex; flex-wrap: wrap; gap: var(--space-3); flex: 2; }
            .highlight-point {
                flex: 1 1 180px;
                background: var(--color-surface-3);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-lg);
                padding: var(--space-4);
                font-size: var(--text-sm);
                color: var(--color-text-muted);
                display: flex;
                align-items: center;
                gap: 8px;
            }
            .highlight-point i { color: var(--color-primary); }
            .panel {
                background: rgba(255, 255, 255, 0.75);
                backdrop-filter: blur(14px);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-xl);
                padding: var(--space-6);
                box-shadow: var(--shadow-sm);
                overflow-x: auto;
            }
            .panel table { width: 100%; border-collapse: collapse; color: var(--color-text); }
            .panel th {
                text-align: left;
                padding: 14px 16px;
                background: var(--color-surface-3);
                font-weight: 700;
                font-size: var(--text-xs);
                text-transform: uppercase;
                letter-spacing: 0.4px;
                color: var(--color-primary-deep);
                border-bottom: 1px solid var(--color-border);
            }
            .panel td { padding: 14px 16px; border-bottom: 1px solid var(--color-border); }
            .panel tr:last-child td { border-bottom: none; }
            .steps { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: var(--space-4); }
            .step-item {
                background: var(--color-surface);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-lg);
                padding: var(--space-5);
                text-align: center;
                font-weight: 600;
                box-shadow: var(--shadow-xs);
                transition: all var(--duration-normal) var(--ease-standard);
            }
            .step-item:hover { transform: translateY(-4px); box-shadow: var(--shadow-sm); }
            .step-number {
                font-family: var(--font-display);
                font-size: var(--text-2xl);
                font-weight: 700;
                color: var(--color-primary);
                display: block;
                margin-bottom: 4px;
            }
            .account-banner {
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                gap: var(--space-3);
                background: var(--color-surface-3);
                border: 1px solid var(--color-border);
                border-radius: var(--radius-xl);
                padding: 16px 24px;
                color: var(--color-text);
                font-weight: 600;
            }
            .account-actions {
                display: flex;
                gap: var(--space-3);
                flex-wrap: wrap;
                justify-content: center;
            }
            @media (max-width: 700px) {
                .hero h1 { font-size: var(--text-2xl); }
                .hero { padding: var(--space-8) var(--space-5); }
            }
            """;
    }

    public static String getUserCssLink() {
        return "<link rel=\"stylesheet\" href=\"/css/user.css\">";
    }

    public static String getAdminCssLink() {
        return "<link rel=\"stylesheet\" href=\"/css/admin.css\">";
    }

    public static String getInstallCssImport() {
        return "@import url('/css/install.css');";
    }

    public static String getWorldCss() {
        return """
            .world-tabs { display: flex; gap: var(--space-2); margin-bottom: var(--space-5); flex-wrap: wrap; }
            .world-tab {
                padding: 9px 20px;
                border-radius: var(--radius-pill);
                border: 1px solid var(--color-border-strong);
                background: var(--color-surface);
                color: var(--color-text-muted);
                cursor: pointer;
                font-size: var(--text-base);
                font-weight: 600;
                transition: all var(--duration-fast) var(--ease-standard);
            }
            .world-tab:hover { border-color: var(--color-primary); color: var(--color-primary-strong); }
            .world-tab.active {
                background: linear-gradient(135deg, var(--color-primary), var(--color-accent-purple));
                color: #fff;
                border-color: transparent;
                box-shadow: var(--shadow-glow);
            }
            .world-loading {
                display: flex;
                align-items: center;
                justify-content: center;
                gap: var(--space-3);
                padding: var(--space-6);
                color: var(--color-text-muted);
            }
            .spinner {
                width: 24px;
                height: 24px;
                border: 3px solid var(--color-primary-soft);
                border-top-color: var(--color-primary);
                border-radius: 50%;
                animation: spin 0.8s linear infinite;
            }
            @keyframes spin { to { transform: rotate(360deg); } }
            .world-end { text-align: center; padding: var(--space-4); color: var(--color-text-faint); font-size: var(--text-sm); }
            .texture-item .like-btn, .texture-item .fav-btn {
                display: inline-flex;
                align-items: center;
                gap: 4px;
                background: none;
                border: none;
                cursor: pointer;
                font-size: var(--text-sm);
                padding: 4px 9px;
                border-radius: var(--radius-pill);
                transition: all var(--duration-fast) var(--ease-standard);
            }
            .texture-item .like-btn { color: var(--color-text-faint); }
            .texture-item .like-btn:hover { color: #E53935; background: #FFF0F0; }
            .texture-item .like-btn.liked { color: #E53935; }
            .texture-item .fav-btn { color: var(--color-text-faint); }
            .texture-item .fav-btn:hover { color: #B45309; background: #FFFBEB; }
            .texture-item .fav-btn.favorited { color: #B45309; }
            .texture-card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
            .texture-card-owner {
                font-size: var(--text-xs);
                color: var(--color-text-faint);
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                flex: 1;
                text-align: left;
            }
            .like-heart { transition: transform var(--duration-fast) var(--ease-standard); }
            .liked .like-heart { animation: heartBeat 0.3s ease; }
            @keyframes heartBeat { 0% { transform: scale(1); } 50% { transform: scale(1.3); } 100% { transform: scale(1); } }
            .visibility-switch { display: flex; align-items: center; gap: var(--space-2); font-size: var(--text-sm); color: var(--color-text-muted); margin: 8px 0; }
            .visibility-switch input[type="checkbox"] {
                width: 42px;
                height: 24px;
                appearance: none;
                background: #D8D2DC;
                border-radius: var(--radius-pill);
                position: relative;
                cursor: pointer;
                transition: all var(--duration-normal) var(--ease-standard);
            }
            .visibility-switch input[type="checkbox"]:checked { background: linear-gradient(135deg, var(--color-primary), var(--color-accent-purple)); }
            .visibility-switch input[type="checkbox"]::before {
                content: '';
                position: absolute;
                width: 18px;
                height: 18px;
                border-radius: 50%;
                background: #fff;
                top: 3px;
                left: 3px;
                transition: all var(--duration-normal) var(--ease-standard);
                box-shadow: var(--shadow-xs);
            }
            .visibility-switch input[type="checkbox"]:checked::before { left: 21px; }
            .shared-section-title {
                font-size: var(--text-md);
                font-weight: 700;
                color: var(--color-text);
                margin-bottom: var(--space-3);
                display: flex;
                align-items: center;
                gap: var(--space-2);
            }
            .shared-section-title i { color: var(--color-primary); }
            @media (max-width: 768px) {
                .world-tabs { justify-content: center; }
                .world-tab { padding: 7px 15px; font-size: var(--text-sm); }
            }
            """;
    }
}
