// JavaNext - Order & Inventory Management System
// API Configuration
const API_BASE_URL = window.location.origin;
const API = {
    PRODUCTS: `${API_BASE_URL}/api/products`,
    ORDERS: `${API_BASE_URL}/orders`,
    LOW_STOCK: `${API_BASE_URL}/api/products/low-stock`
};

// Global State
let products = [];
let orders = [];
let currentPage = 'dashboard';

// Utility Functions
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    const icon = document.getElementById('toast-icon');
    const messageEl = document.getElementById('toast-message');

    // Set icon based on type
    const icons = {
        success: 'fa-check-circle',
        error: 'fa-times-circle',
        warning: 'fa-exclamation-triangle',
        info: 'fa-info-circle'
    };

    icon.className = `fas ${icons[type]} text-2xl`;
    messageEl.textContent = message;
    toast.className = `toast ${type}`;

    setTimeout(() => {
        toast.classList.add('hidden');
    }, 3000);
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(amount);
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function getStatusBadge(status) {
    const statusClasses = {
        PENDING: 'status-pending',
        CONFIRMED: 'status-confirmed',
        COMPLETED: 'status-completed',
        CANCELLED: 'status-cancelled',
        FAILED: 'status-failed'
    };
    return `<span class="status-badge ${statusClasses[status] || 'status-pending'}">${status}</span>`;
}

function getStockClass(available, total) {
    const percentage = (available / total) * 100;
    if (percentage <= 20) return 'stock-low';
    if (percentage <= 50) return 'stock-medium';
    return 'stock-good';
}

// Navigation
function showPage(pageName) {
    // Hide all pages
    document.querySelectorAll('.page').forEach(page => {
        page.classList.remove('active');
    });

    // Remove active class from all nav buttons
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    // Show selected page
    document.getElementById(`page-${pageName}`).classList.add('active');
    document.getElementById(`nav-${pageName}`).classList.add('active');

    currentPage = pageName;

    // Load data for the page
    if (pageName === 'dashboard') {
        loadDashboard();
    } else if (pageName === 'products') {
        loadProducts();
    } else if (pageName === 'orders') {
        loadOrders();
    }
}

// Modal Functions
function showAddProductModal() {
    document.getElementById('modal-overlay').classList.remove('hidden');
    document.getElementById('modal-add-product').classList.remove('hidden');
}

function showCreateOrderModal() {
    document.getElementById('modal-overlay').classList.remove('hidden');
    document.getElementById('modal-create-order').classList.remove('hidden');
    // Add first item by default
    document.getElementById('order-items').innerHTML = '';
    addOrderItem();
}

function showBulkOrderModal() {
    document.getElementById('modal-overlay').classList.remove('hidden');
    document.getElementById('modal-bulk-order').classList.remove('hidden');
    // Add first item template by default
    document.getElementById('bulk-order-items').innerHTML = '';
    addBulkOrderItem();
}

function showOrderDetails(orderId) {
    const order = orders.find(o => o.id === orderId);
    if (!order) return;

    const content = `
        <div class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
                <div>
                    <p class="text-sm text-gray-500">Order ID</p>
                    <p class="font-semibold">${order.id.substring(0, 8)}...</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">Status</p>
                    <p>${getStatusBadge(order.status)}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">Customer ID</p>
                    <p class="font-semibold">${order.customerId.substring(0, 8)}...</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">Total Amount</p>
                    <p class="font-semibold text-lg">${formatCurrency(order.totalAmount)}</p>
                </div>
                <div class="col-span-2">
                    <p class="text-sm text-gray-500">Created At</p>
                    <p class="font-semibold">${formatDate(order.createdAt)}</p>
                </div>
            </div>
        </div>
    `;

    document.getElementById('order-details-content').innerHTML = content;
    document.getElementById('modal-overlay').classList.remove('hidden');
    document.getElementById('modal-order-details').classList.remove('hidden');
}

function closeModals() {
    document.getElementById('modal-overlay').classList.add('hidden');
    document.querySelectorAll('.modal').forEach(modal => {
        modal.classList.add('hidden');
    });
}

// Order Item Management
let orderItemCounter = 0;

function addOrderItem() {
    const itemsContainer = document.getElementById('order-items');
    const itemId = orderItemCounter++;

    const productOptions = products.map(p => 
        `<option value="${p.id}" data-price="${p.price}">${p.name} - ${p.sku} (${formatCurrency(p.price)})</option>`
    ).join('');

    const itemHtml = `
        <div class="border border-gray-300 rounded-lg p-4" id="order-item-${itemId}">
            <div class="grid grid-cols-3 gap-3">
                <div class="col-span-2">
                    <label class="block text-sm font-medium text-gray-700 mb-1">Product</label>
                    <select name="productId" required onchange="updateItemPrice(${itemId})" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
                        <option value="">Select Product</option>
                        ${productOptions}
                    </select>
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Quantity</label>
                    <input type="number" name="quantity" required min="1" value="1" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Price</label>
                    <input type="number" name="price" step="0.01" required readonly class="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50" id="item-price-${itemId}">
                </div>
                <div class="col-span-2 flex items-end">
                    <button type="button" onclick="removeOrderItem(${itemId})" class="text-red-600 hover:text-red-800">
                        <i class="fas fa-trash mr-1"></i>Remove
                    </button>
                </div>
            </div>
        </div>
    `;

    itemsContainer.insertAdjacentHTML('beforeend', itemHtml);
}

function updateItemPrice(itemId) {
    const container = document.getElementById(`order-item-${itemId}`);
    const select = container.querySelector('select[name="productId"]');
    const priceInput = document.getElementById(`item-price-${itemId}`);
    
    const selectedOption = select.options[select.selectedIndex];
    const price = selectedOption.getAttribute('data-price');
    
    if (price) {
        priceInput.value = price;
    }
}

function removeOrderItem(itemId) {
    const item = document.getElementById(`order-item-${itemId}`);
    if (item) {
        item.remove();
    }
}

// Bulk Order Item Management
let bulkItemCounter = 0;

function addBulkOrderItem() {
    const itemsContainer = document.getElementById('bulk-order-items');
    const itemId = bulkItemCounter++;

    const productOptions = products.map(p => 
        `<option value="${p.id}" data-price="${p.price}">${p.name} - ${p.sku} (${formatCurrency(p.price)})</option>`
    ).join('');

    const itemHtml = `
        <div class="border border-gray-300 rounded-lg p-4" id="bulk-item-${itemId}">
            <div class="grid grid-cols-3 gap-3">
                <div class="col-span-2">
                    <label class="block text-sm font-medium text-gray-700 mb-1">Product</label>
                    <select name="productId" required onchange="updateBulkItemPrice(${itemId})" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
                        <option value="">Select Product</option>
                        ${productOptions}
                    </select>
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Quantity</label>
                    <input type="number" name="quantity" required min="1" value="1" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Price</label>
                    <input type="number" name="price" step="0.01" required readonly class="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50" id="bulk-item-price-${itemId}">
                </div>
                <div class="col-span-2 flex items-end">
                    <button type="button" onclick="removeBulkOrderItem(${itemId})" class="text-red-600 hover:text-red-800">
                        <i class="fas fa-trash mr-1"></i>Remove
                    </button>
                </div>
            </div>
        </div>
    `;

    itemsContainer.insertAdjacentHTML('beforeend', itemHtml);
}

function updateBulkItemPrice(itemId) {
    const container = document.getElementById(`bulk-item-${itemId}`);
    const select = container.querySelector('select[name="productId"]');
    const priceInput = document.getElementById(`bulk-item-price-${itemId}`);
    
    const selectedOption = select.options[select.selectedIndex];
    const price = selectedOption.getAttribute('data-price');
    
    if (price) {
        priceInput.value = price;
    }
}

function removeBulkOrderItem(itemId) {
    const item = document.getElementById(`bulk-item-${itemId}`);
    if (item) {
        item.remove();
    }
}

// API Calls - Products
async function loadProducts() {
    try {
        const response = await fetch(API.PRODUCTS);
        if (!response.ok) throw new Error('Failed to load products');
        
        products = await response.json();
        renderProductsTable();
    } catch (error) {
        console.error('Error loading products:', error);
        showToast('Failed to load products', 'error');
        document.getElementById('products-table-body').innerHTML = `
            <tr><td colspan="7" class="px-6 py-4 text-center text-red-500">Error loading products</td></tr>
        `;
    }
}

function renderProductsTable() {
    const tbody = document.getElementById('products-table-body');
    
    if (products.length === 0) {
        tbody.innerHTML = `
            <tr><td colspan="7" class="px-6 py-4 text-center text-gray-500">No products found. Add your first product!</td></tr>
        `;
        return;
    }

    tbody.innerHTML = products.map(product => {
        const stockClass = getStockClass(product.availableQuantity, product.quantity);
        return `
            <tr>
                <td class="px-6 py-4 text-sm font-medium text-gray-900">${product.sku}</td>
                <td class="px-6 py-4 text-sm text-gray-900">${product.name}</td>
                <td class="px-6 py-4 text-sm text-gray-900">${formatCurrency(product.price)}</td>
                <td class="px-6 py-4 text-sm text-gray-900">${product.quantity}</td>
                <td class="px-6 py-4 text-sm text-gray-500">${product.reservedQuantity}</td>
                <td class="px-6 py-4 text-sm ${stockClass}">${product.availableQuantity}</td>
                <td class="px-6 py-4 text-sm">
                    <button onclick="editProduct('${product.id}')" class="text-indigo-600 hover:text-indigo-900 mr-3">
                        <i class="fas fa-edit"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

async function handleAddProduct(event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);

    const productData = {
        sku: formData.get('sku'),
        name: formData.get('name'),
        price: parseFloat(formData.get('price')),
        quantity: parseInt(formData.get('quantity'))
    };

    try {
        const response = await fetch(API.PRODUCTS, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(productData)
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to create product');
        }

        const newProduct = await response.json();
        products.push(newProduct);
        renderProductsTable();
        
        closeModals();
        form.reset();
        showToast('Product created successfully!', 'success');
    } catch (error) {
        console.error('Error creating product:', error);
        showToast(error.message || 'Failed to create product', 'error');
    }
}

function editProduct(productId) {
    showToast('Edit functionality coming soon!', 'info');
}

// API Calls - Orders
async function loadOrders() {
    try {
        const response = await fetch(API.ORDERS);
        if (!response.ok) throw new Error('Failed to load orders');
        
        orders = await response.json();
        renderOrdersTable();
    } catch (error) {
        console.error('Error loading orders:', error);
        showToast('Failed to load orders', 'error');
        document.getElementById('orders-table-body').innerHTML = `
            <tr><td colspan="6" class="px-6 py-4 text-center text-red-500">Error loading orders</td></tr>
        `;
    }
}

function renderOrdersTable() {
    const tbody = document.getElementById('orders-table-body');
    
    if (orders.length === 0) {
        tbody.innerHTML = `
            <tr><td colspan="6" class="px-6 py-4 text-center text-gray-500">No orders found. Create your first order!</td></tr>
        `;
        return;
    }

    tbody.innerHTML = orders.map(order => `
        <tr class="cursor-pointer" onclick="showOrderDetails('${order.id}')">
            <td class="px-6 py-4 text-sm font-mono text-gray-900">${order.id.substring(0, 8)}...</td>
            <td class="px-6 py-4 text-sm font-mono text-gray-500">${order.customerId.substring(0, 8)}...</td>
            <td class="px-6 py-4 text-sm">${getStatusBadge(order.status)}</td>
            <td class="px-6 py-4 text-sm font-semibold text-gray-900">${formatCurrency(order.totalAmount)}</td>
            <td class="px-6 py-4 text-sm text-gray-500">${formatDate(order.createdAt)}</td>
            <td class="px-6 py-4 text-sm">
                <button onclick="event.stopPropagation(); showOrderDetails('${order.id}')" class="text-indigo-600 hover:text-indigo-900">
                    <i class="fas fa-eye"></i> View
                </button>
            </td>
        </tr>
    `).join('');
}

async function handleCreateOrder(event) {
    event.preventDefault();
    const form = event.target;
    const items = [];

    // Collect all order items
    document.querySelectorAll('#order-items > div').forEach(itemDiv => {
        const productId = itemDiv.querySelector('select[name="productId"]').value;
        const quantity = parseInt(itemDiv.querySelector('input[name="quantity"]').value);
        const price = parseFloat(itemDiv.querySelector('input[name="price"]').value);

        if (productId && quantity && price) {
            items.push({ productId, quantity, price });
        }
    });

    if (items.length === 0) {
        showToast('Please add at least one item', 'warning');
        return;
    }

    const orderData = { items };
    const idempotencyKey = generateUUID();

    try {
        const response = await fetch(API.ORDERS, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Idempotency-Key': idempotencyKey
            },
            body: JSON.stringify(orderData)
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to create order');
        }

        const newOrder = await response.json();
        orders.unshift(newOrder);
        renderOrdersTable();
        
        closeModals();
        form.reset();
        showToast('Order created successfully!', 'success');
    } catch (error) {
        console.error('Error creating order:', error);
        showToast(error.message || 'Failed to create order', 'error');
    }
}

async function handleBulkOrder(event) {
    event.preventDefault();
    const form = event.target;
    const orderCount = parseInt(document.getElementById('bulk-order-count').value);
    const itemTemplate = [];

    // Collect item template
    document.querySelectorAll('#bulk-order-items > div').forEach(itemDiv => {
        const productId = itemDiv.querySelector('select[name="productId"]').value;
        const quantity = parseInt(itemDiv.querySelector('input[name="quantity"]').value);
        const price = parseFloat(itemDiv.querySelector('input[name="price"]').value);

        if (productId && quantity && price) {
            itemTemplate.push({ productId, quantity, price });
        }
    });

    if (itemTemplate.length === 0) {
        showToast('Please add at least one item template', 'warning');
        return;
    }

    const bulkOrderData = {
        customerId: generateUUID(),
        orders: Array(orderCount).fill(null).map(() => ({ items: itemTemplate }))
    };

    const idempotencyKey = generateUUID();

    try {
        const response = await fetch(`${API.ORDERS}/bulk`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Idempotency-Key': idempotencyKey
            },
            body: JSON.stringify(bulkOrderData)
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to create bulk orders');
        }

        const result = await response.json();
        
        closeModals();
        form.reset();
        showToast(`Successfully created ${result.successCount} orders!`, 'success');
        
        // Reload orders
        setTimeout(() => loadOrders(), 500);
    } catch (error) {
        console.error('Error creating bulk orders:', error);
        showToast(error.message || 'Failed to create bulk orders', 'error');
    }
}

// Dashboard
async function loadDashboard() {
    try {
        // Load all data
        await Promise.all([loadProducts(), loadOrders()]);
        
        // Update stats
        updateDashboardStats();
        renderRecentOrders();
        renderLowStockAlerts();
    } catch (error) {
        console.error('Error loading dashboard:', error);
        showToast('Failed to load dashboard data', 'error');
    }
}

function updateDashboardStats() {
    const totalOrders = orders.length;
    const completedOrders = orders.filter(o => o.status === 'COMPLETED').length;
    const pendingOrders = orders.filter(o => o.status === 'PENDING').length;
    const lowStockCount = products.filter(p => p.availableQuantity <= 10).length;

    document.getElementById('stat-total-orders').textContent = totalOrders;
    document.getElementById('stat-completed-orders').textContent = completedOrders;
    document.getElementById('stat-pending-orders').textContent = pendingOrders;
    document.getElementById('stat-low-stock').textContent = lowStockCount;
}

function renderRecentOrders() {
    const container = document.getElementById('recent-orders');
    const recentOrders = orders.slice(0, 5);

    if (recentOrders.length === 0) {
        container.innerHTML = '<p class="text-gray-500 text-center py-4">No orders yet</p>';
        return;
    }

    container.innerHTML = recentOrders.map(order => `
        <div class="flex justify-between items-center p-3 bg-gray-50 rounded-lg hover-card cursor-pointer" onclick="showOrderDetails('${order.id}')">
            <div>
                <p class="font-semibold text-sm">Order ${order.id.substring(0, 8)}...</p>
                <p class="text-xs text-gray-500">${formatDate(order.createdAt)}</p>
            </div>
            <div class="text-right">
                <p class="font-semibold">${formatCurrency(order.totalAmount)}</p>
                ${getStatusBadge(order.status)}
            </div>
        </div>
    `).join('');
}

function renderLowStockAlerts() {
    const container = document.getElementById('low-stock-alerts');
    const lowStockProducts = products.filter(p => p.availableQuantity <= 10);

    if (lowStockProducts.length === 0) {
        container.innerHTML = '<p class="text-gray-500 text-center py-4">All products well stocked!</p>';
        return;
    }

    container.innerHTML = lowStockProducts.map(product => `
        <div class="flex justify-between items-center p-3 bg-red-50 rounded-lg border-l-4 border-red-500">
            <div>
                <p class="font-semibold text-sm">${product.name}</p>
                <p class="text-xs text-gray-500">SKU: ${product.sku}</p>
            </div>
            <div class="text-right">
                <p class="font-bold text-red-600">${product.availableQuantity} left</p>
                <p class="text-xs text-gray-500">Reserved: ${product.reservedQuantity}</p>
            </div>
        </div>
    `).join('');
}

// Initialize app
document.addEventListener('DOMContentLoaded', () => {
    console.log('JavaNext UI initialized');
    showPage('dashboard');
});
