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

package im.xz.cn.something.web;

public class Css {

    private static final String COLOR_PINK = "#FF69B4";
    private static final String COLOR_LIGHT_PINK = "#FFB6C1";
    private static final String COLOR_WHITE = "#FFFFFF";
    private static final String COLOR_LIGHT_GRAY = "#F5F5F5";
    private static final String COLOR_LAVENDER = "#E8D5F5";
    private static final String COLOR_TEXT_DARK = "#333333";
    private static final String COLOR_TEXT_MID = "#666666";

    public static String getBaseCss() {
        return """
            * { margin: 0; padding: 0; box-sizing: border-box; }
            
            body {
                font-family: 'Segoe UI', 'Microsoft YaHei', 'Hiragino Sans', sans-serif;
                background: linear-gradient(135deg, #FFF0F5 0%%, #F5F0FF 100%%);
                color: %s;
                min-height: 100vh;
            }
            
            .navbar {
                background: %s;
                box-shadow: 0 2px 12px rgba(255,105,180,0.1);
                position: sticky;
                top: 0;
                z-index: 100;
            }
            .navbar-inner {
                max-width: 1200px;
                margin: 0 auto;
                padding: 0 24px;
                display: flex;
                align-items: center;
                justify-content: space-between;
                height: 64px;
            }
            .navbar-brand {
                display: flex;
                align-items: center;
                gap: 8px;
            }
            .brand-icon {
                font-size: 24px;
                color: %s;
            }
            .brand-text {
                font-size: 20px;
                font-weight: 600;
                color: %s;
            }
            .navbar-links {
                display: flex;
                gap: 8px;
            }
            .nav-link {
                padding: 8px 16px;
                border-radius: 8px;
                text-decoration: none;
                color: %s;
                font-size: 14px;
                transition: all 0.3s ease;
            }
            .nav-link:hover {
                background: #FFF0F5;
                color: %s;
            }
            .nav-link.active {
                background: %s;
                color: %s;
            }
            
            .sidebar {
                width: 240px;
                background: %s;
                border-radius: 16px;
                padding: 16px;
                box-shadow: 0 4px 16px rgba(255,105,180,0.08);
                height: fit-content;
            }
            .sidebar-header {
                padding: 12px 16px;
                font-weight: 600;
                color: %s;
                border-bottom: 1px solid #F0E6FF;
                margin-bottom: 8px;
                display: flex;
                align-items: center;
                gap: 8px;
            }
            .sidebar-item {
                display: block;
                padding: 10px 16px;
                border-radius: 8px;
                text-decoration: none;
                color: %s;
                font-size: 14px;
                transition: all 0.3s ease;
                margin-bottom: 2px;
            }
            .sidebar-item:hover {
                background: #FFF0F5;
                color: %s;
            }
            .sidebar-item.active {
                background: linear-gradient(135deg, %s, %s);
                color: %s;
            }
            
            .card {
                background: %s;
                border-radius: 16px;
                box-shadow: 0 4px 16px rgba(255,105,180,0.08);
                overflow: hidden;
                animation: fadeIn 0.5s ease;
            }
            .card-header {
                padding: 20px 24px;
                border-bottom: 1px solid #F5F0FF;
            }
            .card-title {
                font-size: 18px;
                font-weight: 600;
                color: %s;
            }
            .card-body {
                padding: 24px;
            }
            
            .alert {
                padding: 14px 20px;
                border-radius: 12px;
                margin-bottom: 16px;
                display: flex;
                align-items: center;
                gap: 10px;
                font-size: 14px;
                animation: slideDown 0.3s ease;
            }
            .alert-success {
                background: #E8F8E8;
                color: #2D7D2D;
                border-left: 4px solid #4CAF50;
            }
            .alert-error {
                background: #FFE8E8;
                color: #C62828;
                border-left: 4px solid #F44336;
            }
            .alert-warning {
                background: #FFF8E1;
                color: #F57F17;
                border-left: 4px solid #FF9800;
            }
            .alert-info {
                background: #E3F2FD;
                color: #1565C0;
                border-left: 4px solid #2196F3;
            }
            
            .btn {
                padding: 10px 24px;
                border-radius: 10px;
                border: none;
                font-size: 14px;
                font-weight: 500;
                cursor: pointer;
                transition: all 0.3s ease;
                display: inline-flex;
                align-items: center;
                gap: 6px;
            }
            .btn-primary {
                background: linear-gradient(135deg, %s, %s);
                color: %s;
                box-shadow: 0 4px 12px rgba(255,105,180,0.3);
            }
            .btn-primary:hover {
                transform: translateY(-2px);
                box-shadow: 0 6px 16px rgba(255,105,180,0.4);
            }
            .btn-secondary {
                background: %s;
                color: %s;
                border: 1px solid #E8D5F5;
            }
            .btn-secondary:hover {
                background: #F5F0FF;
            }
            
            .form-input {
                width: 100%%;
                padding: 10px 16px;
                border-radius: 10px;
                border: 2px solid #F0E6FF;
                font-size: 14px;
                transition: all 0.3s ease;
                outline: none;
            }
            .form-input:focus {
                border-color: %s;
                box-shadow: 0 0 0 3px rgba(255,105,180,0.1);
            }
            .form-label {
                display: block;
                font-size: 14px;
                font-weight: 500;
                color: %s;
                margin-bottom: 6px;
            }
            .form-group {
                margin-bottom: 16px;
            }
            
            .table {
                width: 100%%;
                border-collapse: collapse;
            }
            .table th {
                background: #FFF0F5;
                padding: 12px 16px;
                text-align: left;
                font-size: 13px;
                font-weight: 600;
                color: %s;
                border-bottom: 2px solid %s;
            }
            .table td {
                padding: 12px 16px;
                font-size: 14px;
                border-bottom: 1px solid #F5F0FF;
            }
            .table tr:hover {
                background: #FFF8FC;
            }
            
            @keyframes fadeIn {
                from { opacity: 0; transform: translateY(10px); }
                to { opacity: 1; transform: translateY(0); }
            }
            @keyframes slideDown {
                from { opacity: 0; transform: translateY(-10px); }
                to { opacity: 1; transform: translateY(0); }
            }
            
            .container {
                max-width: 1200px;
                margin: 0 auto;
                padding: 24px;
            }
            .admin-layout {
                display: flex;
                gap: 24px;
                max-width: 1200px;
                margin: 24px auto;
                padding: 0 24px;
            }
            .admin-content {
                flex: 1;
            }
            
            .page-footer {
                text-align: center;
                padding: 20px;
                color: #999;
                font-size: 13px;
                margin-top: 40px;
                border-top: 1px solid #eee;
            }
            .page-footer p {
                margin: 0;
            }
            """.formatted(
                COLOR_TEXT_DARK, COLOR_WHITE, COLOR_PINK, COLOR_PINK, COLOR_TEXT_MID, COLOR_PINK,
                COLOR_PINK, COLOR_WHITE, COLOR_WHITE, COLOR_TEXT_MID, COLOR_TEXT_MID, COLOR_PINK,
                COLOR_PINK, COLOR_LIGHT_PINK, COLOR_WHITE, COLOR_WHITE, COLOR_TEXT_DARK,
                COLOR_PINK, COLOR_LIGHT_PINK, COLOR_WHITE, COLOR_PINK, COLOR_TEXT_MID,
                COLOR_PINK, COLOR_TEXT_MID, COLOR_LIGHT_PINK, COLOR_LAVENDER
            );
    }

    public static String getAuthPageCss() {
        return """
            body { display: flex; flex-direction: column; min-height: 100vh; }
            .page-footer { margin-top: auto; }
            .auth-container { flex: 1; display: flex; justify-content: center; align-items: center;
                padding: 40px 24px; }
            .auth-card { background: #FFFFFF; border-radius: 24px; padding: 48px 44px;
                box-shadow: 0 8px 32px rgba(255,105,180,0.12); width: 100%%; max-width: 460px;
                animation: fadeIn 0.5s ease; }
            .auth-header { text-align: center; margin-bottom: 32px; }
            .auth-icon { font-size: 42px; color: #FF69B4; display: block; margin-bottom: 12px; }
            .auth-header h2 { color: #FF69B4; margin-bottom: 8px; font-size: 22px; }
            .auth-header .text-muted { font-size: 14px; }
            .auth-footer { text-align: center; margin-top: 24px; font-size: 14px; color: #999; }
            .auth-footer a { color: #FF69B4; text-decoration: none; }
            .auth-footer a:hover { text-decoration: underline; }
            .form-group { margin-bottom: 20px; }
            .form-input { padding: 12px 16px; font-size: 15px; }
            .form-label { font-size: 14px; margin-bottom: 8px; }
            .btn-block { width: 100%%; justify-content: center; padding: 14px; font-size: 16px;
                margin-top: 8px; border-radius: 12px; }
            .required { color: #FF69B4; }
            .form-row { display: flex; gap: 10px; align-items: center; }
            .msg-area { margin-top: 6px; font-size: 13px; min-height: 18px; }
            .text-muted { color: #999; }
            .form-hint { font-size: 12px; color: #FF90C8; margin-top: 6px; line-height: 1.6; }
            .form-hint span { display: inline-block; background: #FFF0F5; border: 1px solid #FFD6E8;
                border-radius: 4px; padding: 1px 5px; margin: 2px 2px; font-family: monospace; font-size: 12px; color: #FF69B4; }
            """;
    }

    public static String getUserDashboardCss() {
        return """
            .welcome-section { margin-bottom: 24px; }
            .welcome-section h2 { color: #FF69B4; margin-bottom: 8px; }
            .text-muted { color: #999; font-size: 14px; margin: 4px 0; }
            .badge { padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
            .badge-success { background: #E8F8E8; color: #2D7D2D; }
            .badge-warning { background: #FFF8E1; color: #F57F17; }
            .yggdrasil-guide { padding: 4px 0; }
            .guide-step { display: flex; gap: 16px; align-items: flex-start; padding: 16px;
                background: #FFF8FC; border-radius: 12px; margin-bottom: 12px;
                transition: all 0.3s ease; }
            .guide-step:hover { transform: translateX(4px); box-shadow: 0 2px 8px rgba(255,105,180,0.1); }
            .guide-step-num { flex-shrink: 0; width: 32px; height: 32px; border-radius: 50%%;
                background: #FF69B4; color: #fff; display: flex; align-items: center; justify-content: center;
                font-weight: 700; font-size: 15px; }
            .guide-step-body { flex: 1; min-width: 0; }
            .guide-step-title { font-weight: 600; font-size: 16px; color: #333; margin-bottom: 4px; }
            .guide-step-desc { font-size: 14px; color: #666; line-height: 1.6; }
            .guide-step-desc a { color: #FF69B4; text-decoration: none; }
            .guide-step-desc a:hover { text-decoration: underline; }
            .guide-code { margin-top: 8px; padding: 10px 14px; background: #F5F0FF; border-radius: 8px;
                font-family: 'Consolas', 'Courier New', monospace; font-size: 13px; color: #6A1B9A;
                word-break: break-all; user-select: all; border: 1px solid #E8D5F5; }
            .guide-tips { margin-top: 16px; padding: 16px; background: #F0FFF0; border-radius: 12px;
                border: 1px solid #C8E6C9; }
            .guide-tips-title { font-weight: 600; font-size: 15px; color: #2D7D2D; margin-bottom: 8px; }
            .guide-tips ul { margin: 0; padding-left: 20px; }
            .guide-tips li { font-size: 13px; color: #555; line-height: 1.8; }
            .form-row { display: flex; gap: 10px; align-items: center; }
            .inline-form { display: flex; gap: 12px; align-items: center; }
            .msg-area { margin-top: 10px; font-size: 14px; min-height: 20px; }
            .msg-area.success { color: #2D7D2D; }
            .msg-area.error { color: #C62828; }
            .empty-hint { color: #999; text-align: center; padding: 24px; }
            .empty-hint a { color: #FF69B4; }
            .modal-overlay { position: fixed; top: 0; left: 0; width: 100%%; height: 100%%;
                background: rgba(0,0,0,0.4); display: flex; justify-content: center; align-items: center; z-index: 200; }
            .modal-box { background: #fff; border-radius: 16px; padding: 32px; min-width: 360px;
                box-shadow: 0 8px 32px rgba(0,0,0,0.15); animation: fadeIn 0.3s ease; }
            .modal-box h3 { color: #FF69B4; margin-bottom: 16px; }
            .modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }
            .btn-danger { background: #F44336; color: #fff; border: none; }
            .btn-danger:hover { background: #D32F2F; }
            .toast { position: fixed; bottom: 32px; right: 32px; padding: 14px 24px;
                border-radius: 12px; font-size: 14px; font-weight: 500; z-index: 9999;
                box-shadow: 0 8px 24px rgba(0,0,0,0.15); animation: toastSlideIn 0.3s ease; }
            .toast.success { background: #E8F8E8; color: #2D7D2D; border-left: 4px solid #4CAF50; }
            .toast.error { background: #FFE8E8; color: #C62828; border-left: 4px solid #F44336; }
            @keyframes toastSlideIn {
                from { opacity: 0; transform: translateX(40px); }
                to { opacity: 1; transform: translateX(0); }
            }
            @media (max-width: 768px) {
                .sidebar { display: none; }
                .admin-layout { flex-direction: column; }
                .form-row, .inline-form { flex-direction: column; }
            }
            """;
    }

    public static String getTextureCss() {
        return """
            .welcome-section { margin-bottom: 24px; }
            .welcome-section h2 { color: #FF69B4; margin-bottom: 8px; }
            .text-muted { color: #999; font-size: 14px; margin: 4px 0; }
            .badge { padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
            .badge-success { background: #E8F8E8; color: #2D7D2D; }
            .badge-warning { background: #FFF8E1; color: #F57F17; }
            .profile-card { display: flex; align-items: center; gap: 16px; padding: 16px;
                background: #FFF8FC; border-radius: 12px; margin-bottom: 12px;
                transition: all 0.3s ease; }
            .profile-card:hover { transform: translateX(4px); box-shadow: 0 2px 8px rgba(255,105,180,0.1); }
            .profile-avatar { font-size: 32px; color: #FF69B4; }
            .profile-name { font-weight: 600; font-size: 16px; color: #333; }
            .profile-uuid { font-size: 12px; color: #999; font-family: monospace; }
            .profile-model { font-size: 12px; color: #666; }
            .form-row { display: flex; gap: 10px; align-items: center; }
            .inline-form { display: flex; gap: 12px; align-items: center; }
            .msg-area { margin-top: 10px; font-size: 14px; min-height: 20px; }
            .msg-area.success { color: #2D7D2D; }
            .msg-area.error { color: #C62828; }
            .empty-hint { color: #999; text-align: center; padding: 24px; }
            .empty-hint a { color: #FF69B4; }
            .modal-overlay { position: fixed; top: 0; left: 0; width: 100%%; height: 100%%;
                background: rgba(0,0,0,0.4); display: flex; justify-content: center; align-items: center; z-index: 200; }
            .modal-box { background: #fff; border-radius: 16px; padding: 32px; min-width: 360px;
                box-shadow: 0 8px 32px rgba(0,0,0,0.15); animation: fadeIn 0.3s ease; }
            .modal-box h3 { color: #FF69B4; margin-bottom: 16px; }
            .modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }
            .btn-danger { background: #F44336; color: #fff; border: none; }
            .btn-danger:hover { background: #D32F2F; }
            @media (max-width: 768px) {
                .sidebar { display: none; }
                .admin-layout { flex-direction: column; }
                .form-row, .inline-form { flex-direction: column; }
            }
            """;
    }

    public static String getUserCssLink() {
        return "<link rel=\"stylesheet\" href=\"/css/user.css\">";
    }

    public static String getAdminCssLink() {
        return "<link rel=\"stylesheet\" href=\"/css/admin.css\">";
    }

    /** 安装向导 CSS 导入 */
    public static String getInstallCssImport() {
        return "@import url('/css/install.css');";
    }
}
