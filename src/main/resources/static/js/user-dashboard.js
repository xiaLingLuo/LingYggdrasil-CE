(function() {
    document.addEventListener('DOMContentLoaded', function() {
        const cards = document.querySelectorAll('.guide-step, .card');
        cards.forEach(function(card, index) {
            card.style.animationDelay = (index * 0.1) + 's';
            card.classList.add('card-animate');
        });
    });
})();
