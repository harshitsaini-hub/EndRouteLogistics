document.addEventListener("DOMContentLoaded", () => {
    // Register GSAP plugins
    if (typeof gsap !== 'undefined' && typeof ScrollTrigger !== 'undefined') {
        gsap.registerPlugin(ScrollTrigger);
        initAnimations();
    }

    // Shipment Search Logic
    const trackBtn = document.getElementById('trackBtn');
    if (trackBtn) {
        trackBtn.addEventListener('click', (e) => {
            e.preventDefault();
            const input = document.getElementById('trackingInput').value;
            const results = document.getElementById('trackingResults');
            const btn = e.currentTarget;
            
            // Basic validation
            if (!input.trim()) {
                input.focus();
                return;
            }
            
            // Add loading state
            const originalText = btn.innerHTML;
            btn.innerHTML = 'Connecting...';
            btn.style.opacity = '0.8';
            btn.style.pointerEvents = 'none';
            
            // Simulate network request
            setTimeout(() => {
                btn.innerHTML = originalText;
                btn.style.opacity = '1';
                btn.style.pointerEvents = 'auto';
                
                // Show results
                results.classList.add('active');
                
                // Animate trace progress
                gsap.to("#traceProgress", {
                    height: "50%",
                    duration: 1.5,
                    ease: "power2.out",
                    delay: 0.2
                });
                
                // Stagger trace points
                gsap.fromTo(".gsap-trace-point", 
                    { opacity: 0, x: -20 },
                    { opacity: 1, x: 0, duration: 0.5, stagger: 0.2, ease: "power2.out", delay: 0.5 }
                );
            }, 800);
        });
    }
});

function initAnimations() {
    // Navbar Entrance
    gsap.from(".gsap-nav", {
        y: -50,
        opacity: 0,
        duration: 1,
        ease: "power3.out"
    });

    // Hero Section
    if (document.querySelector('.gsap-hero-title')) {
        const heroTl = gsap.timeline();
        
        heroTl.from(".gsap-hero-title", {
            y: 30,
            opacity: 0,
            duration: 0.8,
            ease: "power3.out"
        })
        .from(".gsap-hero-text", {
            y: 20,
            opacity: 0,
            duration: 0.8,
            ease: "power3.out"
        }, "-=0.6")
        .from(".gsap-hero-btn", {
            y: 20,
            opacity: 0,
            duration: 0.6,
            ease: "power2.out"
        }, "-=0.4");
        
        // Parallax Effect on Hero Graphic
        document.addEventListener('mousemove', (e) => {
            const bg = document.getElementById('parallax-bg');
            if (bg) {
                const x = (e.clientX / window.innerWidth - 0.5) * 40;
                const y = (e.clientY / window.innerHeight - 0.5) * 40;
                
                gsap.to(bg, {
                    x: x,
                    y: y,
                    rotation: -5 + (x * 0.05),
                    duration: 1,
                    ease: "power1.out"
                });
            }
        });
    }

    // Scroll Animations
    const fadeUpElements = document.querySelectorAll('.gsap-fade-up');
    fadeUpElements.forEach((el) => {
        gsap.from(el, {
            scrollTrigger: {
                trigger: el,
                start: "top 85%",
            },
            y: 30,
            opacity: 0,
            duration: 0.8,
            ease: "power3.out"
        });
    });

    const cards = document.querySelectorAll('.gsap-card');
    if (cards.length > 0) {
        gsap.from(cards, {
            scrollTrigger: {
                trigger: cards[0],
                start: "top 85%",
            },
            y: 40,
            opacity: 0,
            duration: 0.8,
            stagger: 0.2,
            ease: "power3.out"
        });
    }
}