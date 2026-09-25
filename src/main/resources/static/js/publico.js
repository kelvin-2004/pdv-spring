/* ============================================================
   Marmitas Sousa — interações leves do site público.
   Reveal-on-scroll e sombra da barra ao rolar. Sem dependências.
   ============================================================ */
(function () {
    'use strict';

    function init() {
        revelarAoRolar();
        sombraNavbar();
    }

    // Revela cartões conforme entram na tela (fade + deslize sutil).
    function revelarAoRolar() {
        var alvos = Array.prototype.slice.call(document.querySelectorAll('.card'));
        if (!alvos.length || !('IntersectionObserver' in window)) return;

        var foraDaTela = [];
        alvos.forEach(function (el, i) {
            el.classList.add('ms-reveal');
            if (i < 12) el.style.transitionDelay = (i % 6) * 50 + 'ms';
            var r = el.getBoundingClientRect();
            if (r.top < window.innerHeight && r.bottom > 0) {
                el.classList.add('ms-revealed');
            } else {
                foraDaTela.push(el);
            }
        });

        if (!foraDaTela.length) return;
        var io = new IntersectionObserver(function (entradas) {
            entradas.forEach(function (e) {
                if (e.isIntersecting) {
                    e.target.classList.add('ms-revealed');
                    io.unobserve(e.target);
                }
            });
        }, { threshold: 0.1, rootMargin: '0px 0px -6% 0px' });
        foraDaTela.forEach(function (el) { io.observe(el); });
    }

    // Sombra na barra de navegação ao rolar a página.
    function sombraNavbar() {
        var nav = document.querySelector('.navbar-custom.sticky-top');
        if (!nav) return;
        function atualizar() { nav.classList.toggle('ms-nav-scrolled', window.scrollY > 8); }
        window.addEventListener('scroll', atualizar, { passive: true });
        atualizar();
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();
})();
