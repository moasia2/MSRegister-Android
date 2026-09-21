package com.seagull.msreg

object PxBypassJs {
    val BYPASS_PAYLOAD = """
(function() {
    console.log('[Seagull] Injecting PxBypass...');
    const originalFetch = window.fetch;
    window.fetch = function(url, options) {
        if (typeof url === 'string' && (url.includes('arkose') || url.includes('funcaptcha') || url.includes('px2') || url.includes('client-api.arkoselabs.com'))) {
            console.log('[Bypass] Intercepted fetch: ' + url);
            return Promise.resolve(new Response(JSON.stringify({
                token: "bypass_token_" + Date.now(), success: true
            }), { status: 200, headers: { 'Content-Type': 'application/json' } }));
        }
        return originalFetch.apply(this, arguments);
    };
    const originalOpen = XMLHttpRequest.prototype.open;
    const originalSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function(method, url) { this._url = url; return originalOpen.apply(this, arguments); };
    XMLHttpRequest.prototype.send = function(body) {
        if (this._url && (this._url.includes('arkose') || this._url.includes('funcaptcha') || this._url.includes('px2'))) {
            Object.defineProperty(this, 'responseText', { value: JSON.stringify({ token: "bypass_xhr", success: true }) });
            Object.defineProperty(this, 'status', { value: 200 });
            this.readyState = 4;
            if (this.onreadystatechange) this.onreadystatechange();
            if (this.onload) this.onload();
            return;
        }
        return originalSend.apply(this, arguments);
    };
    if (window.Enforcement) window.Enforcement.onSuccess && window.Enforcement.onSuccess({ token: "forced_bypass" });
    if (window.ArkoseEnforcement) window.ArkoseEnforcement.onSuccess && window.ArkoseEnforcement.onSuccess({ token: "forced_bypass" });
})();
""".trimIndent()

    private val REACT_FILL_HELPER = """
function setNativeValue(element, value) {
    const valueSetter = Object.getOwnPropertyDescriptor(element, 'value').set;
    const prototype = Object.getPrototypeOf(element);
    const prototypeValueSetter = Object.getOwnPropertyDescriptor(prototype, 'value').set;
    if (valueSetter && valueSetter !== prototypeValueSetter) prototypeValueSetter.call(element, value);
    else if (valueSetter) valueSetter.call(element, value);
    element.dispatchEvent(new Event('input', { bubbles: true }));
    element.dispatchEvent(new Event('change', { bubbles: true }));
}
function clickEl(el) {
    if(el) {
        el.dispatchEvent(new MouseEvent('mousedown', {bubbles: true}));
        el.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}));
        el.dispatchEvent(new MouseEvent('click', {bubbles: true}));
        if(el.click) el.click();
    }
}
""".trimIndent()

    fun fillEmail(email: String) = "$REACT_FILL_HELPER (function() { let el = document.querySelector('input[name=\"MemberName\"]') || document.querySelector('input[type=\"email\"]'); if(el) setNativeValue(el, '$email'); })();"
    fun fillPassword(pwd: String) = "$REACT_FILL_HELPER (function() { let el = document.querySelector('input[name=\"Password\"]') || document.querySelector('input[type=\"password\"]'); if(el) setNativeValue(el, '$pwd'); })();"
    fun fillDetails(first: String, last: String) = """
$REACT_FILL_HELPER
(function() {
    let fn = document.querySelector('input[name="FirstName"]');
    let ln = document.querySelector('input[name="LastName"]');
    if(fn) setNativeValue(fn, '$first');
    if(ln) setNativeValue(ln, '$last');
    let yEl = document.querySelector('input[name="BirthYear"]') || document.getElementById('BirthYear');
    if(yEl) setNativeValue(yEl, (1990 + Math.floor(Math.random() * 10)).toString());
    let mEl = document.querySelector('input[name="BirthMonth"]') || document.getElementById('BirthMonth');
    if(mEl) setNativeValue(mEl, (Math.floor(Math.random() * 12) + 1).toString());
    let dEl = document.querySelector('input[name="BirthDay"]') || document.getElementById('BirthDay');
    if(dEl) setNativeValue(dEl, (Math.floor(Math.random() * 28) + 1).toString());
})();
""".trimIndent()
    fun fillOtp(code: String) = """
(function() {
    let inputs = document.querySelectorAll('input[type="tel"], input[type="number"], input[name="code"], input[name="code0"], input[type="text"]');
    let otpInputs = Array.from(inputs).filter(el => el.maxLength == 1 || el.type == 'tel' || el.id.includes('iShowEmpty') || el.id.includes('code'));
    if(otpInputs.length >= 6 && '$code'.length >= 6) {
        for(let i=0; i<6; i++) {
            if(otpInputs[i]) {
                otpInputs[i].focus();
                otpInputs[i].value = '$code'[i];
                otpInputs[i].dispatchEvent(new Event('input', { bubbles: true }));
            }
        }
    } else if(inputs.length > 0) {
        setNativeValue(inputs[0], '$code');
    }
})();
""".trimIndent()
    fun clickNext() = "$REACT_FILL_HELPER (function() { let btn = document.querySelector('input[type=\"submit\"]') || document.querySelector('button[type=\"submit\"]') || document.getElementById('iSignupAction') || document.querySelector('button#iNext'); if(btn) clickEl(btn); })();"
}