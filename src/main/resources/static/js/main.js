
(function ($) {
    "use strict";

    /*[ Load page ]
    ===========================================================*/
    $(".animsition").animsition({
        inClass: 'fade-in',
        outClass: 'fade-out',
        inDuration: 1500,
        outDuration: 800,
        linkElement: '.animsition-link',
        loading: true,
        loadingParentElement: 'html',
        loadingClass: 'animsition-loading-1',
        loadingInner: '<div class="loader05"></div>',
        timeout: false,
        timeoutCountdown: 5000,
        onLoadEvent: true,
        browser: [ 'animation-duration', '-webkit-animation-duration'],
        overlay : false,
        overlayClass : 'animsition-overlay-slide',
        overlayParentElement : 'html',
        transition: function(url){ window.location.href = url; }
    });
    
    /*[ Back to top ]
    ===========================================================*/
    var windowH = $(window).height()/2;

    $(window).on('scroll',function(){
        if ($(this).scrollTop() > windowH) {
            $("#myBtn").css('display','flex');
        } else {
            $("#myBtn").css('display','none');
        }
    });

    $('#myBtn').on("click", function(){
        $('html, body').animate({scrollTop: 0}, 300);
    });


    /*==================================================================
    [ Fixed Header ]*/
    var headerDesktop = $('.container-menu-desktop');
    var wrapMenu = $('.wrap-menu-desktop');

    if($('.top-bar').length > 0) {
        var posWrapHeader = $('.top-bar').height();
    }
    else {
        var posWrapHeader = 0;
    }
    

    if($(window).scrollTop() > posWrapHeader) {
        $(headerDesktop).addClass('fix-menu-desktop');
        $(wrapMenu).css('top',0); 
    }  
    else {
        $(headerDesktop).removeClass('fix-menu-desktop');
        $(wrapMenu).css('top',posWrapHeader - $(this).scrollTop()); 
    }

    $(window).on('scroll',function(){
        if($(this).scrollTop() > posWrapHeader) {
            $(headerDesktop).addClass('fix-menu-desktop');
            $(wrapMenu).css('top',0); 
        }  
        else {
            $(headerDesktop).removeClass('fix-menu-desktop');
            $(wrapMenu).css('top',posWrapHeader - $(this).scrollTop()); 
        } 
    });


    /*==================================================================
    [ Menu mobile ]*/
    $('.btn-show-menu-mobile').on('click', function(){
        $(this).toggleClass('is-active');
        $('.menu-mobile').slideToggle();
    });

    var arrowMainMenu = $('.arrow-main-menu-m');

    for(var i=0; i<arrowMainMenu.length; i++){
        $(arrowMainMenu[i]).on('click', function(){
            $(this).parent().find('.sub-menu-m').slideToggle();
            $(this).toggleClass('turn-arrow-main-menu-m');
        })
    }

    $(window).resize(function(){
        if($(window).width() >= 992){
            if($('.menu-mobile').css('display') == 'block') {
                $('.menu-mobile').css('display','none');
                $('.btn-show-menu-mobile').toggleClass('is-active');
            }

            $('.sub-menu-m').each(function(){
                if($(this).css('display') == 'block') { console.log('hello');
                    $(this).css('display','none');
                    $(arrowMainMenu).removeClass('turn-arrow-main-menu-m');
                }
            });
                
        }
    });


    /*==================================================================
    [ Show / hide modal search ]*/
    $('.js-show-modal-search').on('click', function(){
        $('.modal-search-header').addClass('show-modal-search');
        $(this).css('opacity','0');
    });

    $('.js-hide-modal-search').on('click', function(){
        $('.modal-search-header').removeClass('show-modal-search');
        $('.js-show-modal-search').css('opacity','1');
    });

    $('.container-search-header').on('click', function(e){
        e.stopPropagation();
    });


    /*==================================================================
    [ Isotope ]*/
    var $topeContainer = $('.isotope-grid');
    var $filter = $('.filter-tope-group');

    // filter items on button click
    $filter.each(function () {
        $filter.on('click', 'button', function () {
            var filterValue = $(this).attr('data-filter');
            $topeContainer.isotope({filter: filterValue});
        });
        
    });

    // init Isotope
    $(window).on('load', function () {
        var $grid = $topeContainer.each(function () {
            $(this).isotope({
                itemSelector: '.isotope-item',
                layoutMode: 'fitRows',
                percentPosition: true,
                animationEngine : 'best-available',
                masonry: {
                    columnWidth: '.isotope-item'
                }
            });
        });
    });

    var isotopeButton = $('.filter-tope-group button');

    $(isotopeButton).each(function(){
        $(this).on('click', function(){
            for(var i=0; i<isotopeButton.length; i++) {
                $(isotopeButton[i]).removeClass('how-active1');
            }

            $(this).addClass('how-active1');
        });
    });

    /*==================================================================
    [ Filter / Search product ]*/
    $('.js-show-filter').on('click',function(){
        $(this).toggleClass('show-filter');
        $('.panel-filter').slideToggle(400);

        if($('.js-show-search').hasClass('show-search')) {
            $('.js-show-search').removeClass('show-search');
            $('.panel-search').slideUp(400);
        }    
    });

    $('.js-show-search').on('click',function(){
        $(this).toggleClass('show-search');
        $('.panel-search').slideToggle(400);

        if($('.js-show-filter').hasClass('show-filter')) {
            $('.js-show-filter').removeClass('show-filter');
            $('.panel-filter').slideUp(400);
        }    
    });




    /*==================================================================
    [ Cart ]*/
    $('.js-show-cart').on('click',function(){
        $('.js-panel-cart').addClass('show-header-cart');
    });

    $('.js-hide-cart').on('click',function(){
        $('.js-panel-cart').removeClass('show-header-cart');
    });

    /*==================================================================
    [ Cart ]*/
    $('.js-show-sidebar').on('click',function(){
        $('.js-sidebar').addClass('show-sidebar');
    });

    $('.js-hide-sidebar').on('click',function(){
        $('.js-sidebar').removeClass('show-sidebar');
    });

    /*==================================================================
    [ +/- num product ]*/
    $('.btn-num-product-down').on('click', function(){
        var numProduct = Number($(this).next().val());
        if(numProduct > 0) $(this).next().val(numProduct - 1);
    });

    $('.btn-num-product-up').on('click', function(){
        var numProduct = Number($(this).prev().val());
        $(this).prev().val(numProduct + 1);
    });

    /*==================================================================
    [ Rating ]*/
    $('.wrap-rating').each(function(){
        var item = $(this).find('.item-rating');
        var rated = -1;
        var input = $(this).find('input');
        $(input).val(0);

        $(item).on('mouseenter', function(){
            var index = item.index(this);
            var i = 0;
            for(i=0; i<=index; i++) {
                $(item[i]).removeClass('zmdi-star-outline');
                $(item[i]).addClass('zmdi-star');
            }

            for(var j=i; j<item.length; j++) {
                $(item[j]).addClass('zmdi-star-outline');
                $(item[j]).removeClass('zmdi-star');
            }
        });

        $(item).on('click', function(){
            var index = item.index(this);
            rated = index;
            $(input).val(index+1);
        });

        $(this).on('mouseleave', function(){
            var i = 0;
            for(i=0; i<=rated; i++) {
                $(item[i]).removeClass('zmdi-star-outline');
                $(item[i]).addClass('zmdi-star');
            }

            for(var j=i; j<item.length; j++) {
                $(item[j]).addClass('zmdi-star-outline');
                $(item[j]).removeClass('zmdi-star');
            }
        });
    });
    
    /*==================================================================
    [ Quick view modal ]*/
    var quickViewModal = $('.js-modal1');
    var quickViewName = quickViewModal.find('.js-name-detail');
    var quickViewPrice = quickViewModal.find('.js-price-detail');
    var quickViewDescription = quickViewModal.find('.js-description-detail');
    var quickViewVariantList = quickViewModal.find('.js-variant-list');
    var quickViewAddToCartForm = $('#quickViewAddToCartForm');
    var quickViewDefaultImage = '/images/product-detail-01.jpg';
    var quickViewCurrencyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

    $('.js-show-modal1').on('click',function(e){
        e.preventDefault();
        var productId = $(this).data('product-id');
        if (!productId) {
            quickViewModal.addClass('show-modal1');
            return;
        }

        fetch('/api/products/' + productId + '/quick-view')
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('Failed to load product quick view');
                }
                return response.json();
            })
            .then(function(data) {
                renderQuickView(data);
                quickViewModal.addClass('show-modal1');
            })
            .catch(function(error) {
                console.error(error);
                resetQuickView();
                alert('Unable to load product information. Please try again later.');
            });
    });

    quickViewVariantList.on('click', '.variant-option', function(){
        selectVariantButton($(this));
    });

    $('.js-size-option').on('click', function () {
        var $btn = $(this);
        var size = $btn.data('size');
        $('.js-size-option').removeClass('how-active1');
        $btn.addClass('how-active1');
        $('#quickViewSelectedSize').val(size);
        $('.size-select-hint').hide();
    });

    quickViewAddToCartForm.on('submit', function(e){
        e.preventDefault();
        var selectedVariant = $(this).find('input[name="variantId"]').val();
        if (!selectedVariant) {
            alert('Please choose a variant before adding to cart.');
            return;
        }
        var selectedSize = $('#quickViewSelectedSize').val();
        if (!selectedSize) {
            alert('Please choose a size before adding to cart.');
            $('.size-select-hint').show();
            return;
        }
        submitAddToCart($(this));
    });
    $('.js-hide-modal1').on('click',function(){
        quickViewModal.removeClass('show-modal1');
        resetQuickView();
    });

    function renderQuickView(product) {
        resetQuickView();
        quickViewName.text(product.name || '');
        quickViewDescription.text(product.description || '');

        var variants = Array.isArray(product.variants) ? product.variants : [];
        if (variants.length === 0) {
            quickViewPrice.text('Contact for price');
            initQuickViewSlider([resolveImagePath(product.mainImage)]);
            quickViewVariantList.append('<span class="stext-109 cl3 m-b-10">Out of stock</span>');
            return;
        }

        quickViewPrice.text(formatPriceRange(variants));

        var sliderImages = variants.map(function(variant){
            return resolveImagePath(variant.variantImage, product.mainImage);
        });

        populateVariantButtons(variants);
        // Khởi tạo slider sau khi buttons đã được tạo và variant đầu tiên đã được chọn
        initQuickViewSlider(sliderImages, function() {
            // Sau khi slider khởi tạo xong, chuyển đến slide của variant đầu tiên
            if (variants.length > 0) {
                var slick3 = quickViewModal.find('.wrap-slick3 .slick3');
                if (slick3.hasClass('slick-initialized')) {
                    slick3.slick('slickGoTo', 0);
                }
            }
        });
    }

    function populateVariantButtons(variants) {
        quickViewVariantList.empty();
        variants.forEach(function(variant, index){
            var button = $('<button type="button"></button>')
                .addClass('variant-option flex-c-m stext-104 cl6 size-104 bor2 hov-btn1 trans-04 m-r-8 m-b-8')
                .text(variant.sku || ('Variant ' + (index + 1)))
                .data('variant', variant)
                .data('slide-index', index);

            quickViewVariantList.append(button);
        });
        
        // Tự động chọn variant đầu tiên và ẩn hint
        if (variants.length > 0) {
            var firstButton = quickViewVariantList.find('.variant-option').first();
            if (firstButton.length > 0) {
                selectVariantButton(firstButton, true);
                quickViewModal.find('.variant-select-hint').hide();
            }
        } else {
            quickViewModal.find('.variant-select-hint').show();
        }
    }

    function selectVariantButton($button, initializing, suppressSlideSync) {
        if (!$button || $button.length === 0) {
            return;
        }

        initializing = !!initializing;
        suppressSlideSync = !!suppressSlideSync;

        quickViewVariantList.find('.variant-option').removeClass('how-active1 variant-option-active');
        $button.addClass('how-active1 variant-option-active');

        var variant = $button.data('variant');
        if (!variant) {
            return;
        }
        
        // Set price
        if (variant.price !== undefined && variant.price !== null) {
            quickViewPrice.text(formatCurrency(variant.price));
        }
        
        // Set variantId - đảm bảo có giá trị
        var variantId = variant.variantId || variant.variantID || '';
        if (variantId) {
            quickViewAddToCartForm.find('input[name="variantId"]').val(variantId);
            quickViewModal.find('.variant-select-hint').hide();
        } else {
            quickViewAddToCartForm.find('input[name="variantId"]').val('');
            quickViewModal.find('.variant-select-hint').show();
        }

        // Chuyển slide nếu slider đã khởi tạo
        var slideIndex = $button.data('slide-index');
        var slick3 = quickViewModal.find('.wrap-slick3 .slick3');
        if (!suppressSlideSync && slick3.hasClass('slick-initialized') && typeof slideIndex === 'number') {
            if (initializing) {
                // Nếu đang khởi tạo, chờ một chút rồi mới chuyển slide
                setTimeout(function() {
                    slick3.slick('slickGoTo', slideIndex);
                }, 50);
            } else {
                slick3.slick('slickGoTo', slideIndex);
            }
        }
    }

    function resetQuickView() {
        quickViewVariantList.empty();
        quickViewName.text('');
        quickViewPrice.text('');
        quickViewDescription.text('');
        quickViewAddToCartForm.find('input[name="variantId"]').val('');
        quickViewAddToCartForm.find('input[name="quantity"]').val(1);
        $('#quickViewSelectedSize').val('');
        $('.js-size-option').removeClass('how-active1');
        $('.size-select-hint').show();

        var slick3 = quickViewModal.find('.wrap-slick3 .slick3');
        if (slick3.hasClass('slick-initialized')) {
            slick3.slick('unslick');
        }
        slick3.empty();
        quickViewModal.find('.wrap-slick3-dots').empty();
        quickViewModal.find('.wrap-slick3-arrows').empty();
    }

    function initQuickViewSlider(images, callback) {
        var slick3 = quickViewModal.find('.wrap-slick3 .slick3');
        var arrowsContainer = quickViewModal.find('.wrap-slick3-arrows');
        var dotsContainer = quickViewModal.find('.wrap-slick3-dots');

        if (slick3.hasClass('slick-initialized')) {
            slick3.slick('unslick');
        }

        slick3.empty();
        arrowsContainer.empty();
        dotsContainer.empty();

        var slides = images && images.length ? images : [quickViewDefaultImage];
        slides.forEach(function(image, index){
            var resolved = resolveImagePath(image);
            var slide = $('<div></div>')
                .addClass('item-slick3')
                .attr('data-thumb', resolved)
                .attr('data-variant-index', index);

            var wrapPic = $('<div></div>').addClass('wrap-pic-w pos-relative');
            wrapPic.append($('<img>').attr('src', resolved).attr('alt', 'IMG-PRODUCT'));
            wrapPic.append(
                $('<a></a>')
                    .addClass('flex-c-m size-108 how-pos1 bor0 fs-16 cl10 bg0 hov-btn3 trans-04')
                    .attr('href', resolved)
                    .append('<i class="fa fa-expand"></i>')
            );
            slide.append(wrapPic);
            slick3.append(slide);
        });

        var initCallback = (typeof callback === 'function') ? callback : null;
        var initCallbackInvoked = false;
        function invokeInitCallbackOnce() {
            if (initCallback && !initCallbackInvoked) {
                initCallbackInvoked = true;
                initCallback();
            }
        }
        if (initCallback) {
            slick3.off('init.quickViewCallback').on('init.quickViewCallback', function() {
                invokeInitCallbackOnce();
            });
        }

        slick3.slick({
            slidesToShow: 1,
            slidesToScroll: 1,
            fade: true,
            infinite: true,
            autoplay: false,
            autoplaySpeed: 6000,
            arrows: true,
            appendArrows: arrowsContainer,
            prevArrow:'<button class="arrow-slick3 prev-slick3"><i class="fa fa-angle-left" aria-hidden="true"></i></button>',
            nextArrow:'<button class="arrow-slick3 next-slick3"><i class="fa fa-angle-right" aria-hidden="true"></i></button>',
            dots: true,
            appendDots: dotsContainer,
            dotsClass:'slick3-dots',
            customPaging: function(slick, index) {
                var portrait = $(slick.$slides[index]).data('thumb');
                return '<img src="' + portrait + '"/><div class="slick3-dot-overlay"></div>';
            }
        });

        slick3.off('afterChange.quickView').on('afterChange.quickView', function(event, slick, currentSlide) {
            var $slide = $(slick.$slides[currentSlide]);
            var variantIndex = $slide.data('variant-index');
            if (typeof variantIndex !== 'number') {
                return;
            }
            var targetButton = quickViewVariantList.find('.variant-option').eq(variantIndex);
            if (targetButton.length && !targetButton.hasClass('variant-option-active')) {
                selectVariantButton(targetButton, false, true);
            }
        });
        
        // Nếu vì lý do nào đó event init không bắn, fallback gọi callback
        if (initCallback && slick3.hasClass('slick-initialized')) {
            setTimeout(invokeInitCallbackOnce, 50);
        }
    }

    function resolveImagePath(path, fallback) {
        if (!path || path.length === 0) {
            if (fallback && fallback.length) {
                return resolveImagePath(fallback);
            }
            return quickViewDefaultImage;
        }
        if (path.charAt(0) === '/') {
            return path;
        }
        if (path.indexOf('http://') === 0 || path.indexOf('https://') === 0) {
            return path;
        }
        return '/' + path.replace(/^\/?static[\\/]/, '');
    }

    function formatCurrency(value) {
        var numberValue = Number(value);
        if (!isFinite(numberValue)) {
            return 'Contact for price';
        }
        return quickViewCurrencyFormatter.format(numberValue);
    }

    function formatPriceRange(variants) {
        var prices = variants
            .map(function(v){ return Number(v.price); })
            .filter(function(price){ return isFinite(price); });

        if (prices.length === 0) {
            return 'Contact for price';
        }

        var min = Math.min.apply(null, prices);
        var max = Math.max.apply(null, prices);

        if (min === max) {
            return quickViewCurrencyFormatter.format(min);
        }
        return quickViewCurrencyFormatter.format(min) + ' - ' + quickViewCurrencyFormatter.format(max);
    }

    /*==================================================================
    [ Product detail variant selection ]*/
    var productDetailSection = $('.js-product-detail');
    if (productDetailSection.length) {
        var detailVariantButtons = productDetailSection.find('.variant-option-detail');
        var detailVariantList = productDetailSection.find('.js-variant-list-detail');
        var detailPriceElement = productDetailSection.find('.js-price-detail');
        var detailAddToCartForm = $('#productDetailAddToCartForm');
        var detailSlick = productDetailSection.find('.wrap-slick3 .slick3');

        detailVariantButtons.on('click', function(){
            var $button = $(this);
            detailVariantButtons.removeClass('how-active1 variant-option-active');
            $button.addClass('how-active1 variant-option-active');

            var price = $button.data('price');
            if (price !== undefined && price !== null && price !== '') {
                detailPriceElement.text(formatCurrency(price));
            }

            detailAddToCartForm.find('input[name="variantId"]').val($button.data('variant-id'));

            var slideIndex = $button.data('slide-index');
            if (detailSlick.hasClass('slick-initialized') && typeof slideIndex === 'number') {
                detailSlick.slick('slickGoTo', slideIndex);
            }
        });

        detailVariantList.on('keydown', '.variant-option-detail', function(event){
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                $(this).trigger('click');
            }
        });

    detailAddToCartForm.on('submit', function(e){
        e.preventDefault();
        var selectedVariant = $(this).find('input[name="variantId"]').val();
        if (!selectedVariant) {
            alert('Please choose a SKU before adding to cart.');
            return;
        }
        submitAddToCart($(this));
    });
    }

    loadCartSummary();

    function submitAddToCart($form) {
        var formElement = $form.get(0);
        if (!formElement) {
            return;
        }

        var formData = new FormData(formElement);
        fetch($form.attr('action') || '/cart/add', {
            method: 'POST',
            body: formData,
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
            .then(function(response){
                if (!response.ok) {
                    throw new Error('Unable to add product to cart');
                }
                return response.json();
            })
            .then(function(summary){
                updateHeaderCart(summary);
                $form.find('input[name="quantity"]').val(1);
                $('.js-panel-cart').addClass('show-header-cart');
            })
            .catch(function(error){
                console.error(error);
                alert('Unable to add product to cart. Please try again later.');
            });
    }

    function loadCartSummary() {
        fetch('/cart/summary', {
            method: 'GET',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
            .then(function(response){
                if (!response.ok) {
                    throw new Error('Unable to load cart summary');
                }
                return response.json();
            })
            .then(function(summary){
                updateHeaderCart(summary);
            })
            .catch(function(error){
                console.error(error);
            });
    }

    function removeCartItem(cartItemId) {
        fetch('/cart/remove/' + cartItemId, {
            method: 'DELETE',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
            .then(function(response){
                if (!response.ok) {
                    throw new Error('Unable to remove item from cart');
                }
                return response.json();
            })
            .then(function(summary){
                updateHeaderCart(summary);
            })
            .catch(function(error){
                console.error(error);
                alert('Unable to remove item from cart. Please try again.');
            });
    }

    function updateHeaderCart(summary) {
        summary = summary || {};
        var items = Array.isArray(summary.items) ? summary.items : [];
        var listContainer = $('.header-cart-wrapitem.w-full');
        var totalElement = $('.header-cart-total');
        var cartNoti = $('.icon-header-noti.js-show-cart');

        if (!listContainer.length) {
            return;
        }

        listContainer.empty();

        if (items.length === 0) {
            listContainer.append('<li class="header-cart-item flex-w flex-t m-b-12"><div class="header-cart-item-txt p-t-8"><span class="stext-109 cl3">Your cart is empty</span></div></li>');
        } else {
            items.forEach(function(item){
                // Resolve image path - ưu tiên variant image, fallback về default
                var imagePath = item.image || '';
                var resolvedImage = resolveImagePath(imagePath);
                if (!resolvedImage || resolvedImage === quickViewDefaultImage) {
                    // Nếu không có ảnh, dùng default
                    resolvedImage = quickViewDefaultImage;
                }
                var line = $('<li class="header-cart-item flex-w flex-t m-b-12"></li>');
                var imgWrapper = $('<div class="header-cart-item-img"></div>');
                var img = $('<img>').attr('src', resolvedImage).attr('alt', item.productName || 'Product');
                img.on('error', function() {
                    // Nếu ảnh lỗi, dùng default image
                    $(this).attr('src', quickViewDefaultImage);
                });
                imgWrapper.append(img);
                var textWrapper = $('<div class="header-cart-item-txt p-t-8 flex-w flex-sb-m" style="width:100%;"></div>');
                var textContent = $('<div class="flex-col-l" style="flex:1;"></div>');
                
                // Hiển thị product name
                textContent.append(
                    $('<a class="header-cart-item-name m-b-4 hov-cl1 trans-04"></a>').text(item.productName || 'Product')
                );
                
                // Hiển thị variant label hoặc SKU để phân biệt các variant
                if (item.variantLabel) {
                    textContent.append(
                        $('<span class="d-block stext-111 cl3 m-b-4"></span>').text(item.variantLabel)
                    );
                } else if (item.sku) {
                    textContent.append(
                        $('<span class="d-block stext-111 cl3 m-b-4"></span>').text('SKU: ' + item.sku)
                    );
                }
                
                // Hiển thị quantity và price
                textContent.append(
                    $('<span class="header-cart-item-info stext-109 cl3"></span>').text(item.quantity + ' x ' + formatCurrency(item.price))
                );
                
                textWrapper.append(textContent);
                
                // Thêm nút xóa
                var deleteBtn = $('<button type="button" class="header-cart-item-delete flex-c-m trans-04" style="background:none;border:none;cursor:pointer;color:#999;font-size:18px;padding:4px 8px;margin-left:8px;" title="Remove item"></button>')
                    .html('<i class="zmdi zmdi-close"></i>')
                    .data('cart-item-id', item.cartItemId);
                textWrapper.append(deleteBtn);
                
                deleteBtn.on('click', function(e){
                    e.preventDefault();
                    e.stopPropagation();
                    var cartItemId = $(this).data('cart-item-id');
                    if (cartItemId) {
                        removeCartItem(cartItemId);
                    }
                });

                line.append(imgWrapper);
                line.append(textWrapper);
                listContainer.append(line);
            });
        }

        if (totalElement.length) {
            var totalText = 'Total: ' + formatCurrency(summary.totalAmount || 0);
            totalElement.text(totalText);
        }

        if (cartNoti.length) {
            cartNoti.attr('data-notify', summary.totalQuantity || 0);
        }
    }

    function updateWishlistNavBadge(count) {
        var badge = (typeof count === 'number' && count >= 0) ? count : 0;
        document.querySelectorAll('.wishlist-nav-link').forEach(function (el) {
            if (el) {
                el.setAttribute('data-notify', badge);
            }
        });
    }

    function refreshWishlistCount() {
        fetch('/wishlist/count', {
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
            .then(function (res) { return res.ok ? res.json() : { count: 0 }; })
            .then(function (data) {
                var count = (data && typeof data.count === 'number') ? data.count : 0;
                updateWishlistNavBadge(count);
            })
            .catch(function () {
                updateWishlistNavBadge(0);
            });
    }

    window.refreshWishlistCount = refreshWishlistCount;
    document.addEventListener('DOMContentLoaded', refreshWishlistCount);

})(jQuery);