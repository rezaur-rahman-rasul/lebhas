import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/brands/state/brand_controller.dart';
import 'package:lebhas_creative_maker/features/product_services/domain/product_service_models.dart';
import 'package:lebhas_creative_maker/features/product_services/state/product_service_controller.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_button.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_text_field.dart';

class ProductServiceFormScreen extends ConsumerStatefulWidget {
  final String? productServiceId;
  final String? initialBrandId;

  const ProductServiceFormScreen({
    super.key,
    this.productServiceId,
    this.initialBrandId,
  });

  @override
  ConsumerState<ProductServiceFormScreen> createState() => _ProductServiceFormScreenState();
}

class _ProductServiceFormScreenState extends ConsumerState<ProductServiceFormScreen> {
  final _formKey = GlobalKey<FormState>();
  
  String? _selectedBrandId;
  late TextEditingController _nameController;
  late TextEditingController _descriptionController;
  late TextEditingController _categoryController;
  late TextEditingController _targetAudienceController;
  late TextEditingController _sellingPointsController;

  @override
  void initState() {
    super.initState();
    _selectedBrandId = widget.initialBrandId;
    _nameController = TextEditingController();
    _descriptionController = TextEditingController();
    _categoryController = TextEditingController();
    _targetAudienceController = TextEditingController();
    _sellingPointsController = TextEditingController();

    if (widget.productServiceId != null) {
      _loadProductData();
    }
  }

  void _loadProductData() {
    final productAsync = ref.read(productServiceDetailsProvider(widget.productServiceId!));
    productAsync.whenData((product) {
      setState(() {
        _selectedBrandId = product.brandId;
        _nameController.text = product.name;
        _descriptionController.text = product.description;
        _categoryController.text = product.category;
        _targetAudienceController.text = product.targetAudience;
        _sellingPointsController.text = product.sellingPoints;
      });
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    _categoryController.dispose();
    _targetAudienceController.dispose();
    _sellingPointsController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_selectedBrandId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please select a brand')),
      );
      return;
    }

    final request = CreateProductServiceRequest(
      brandId: _selectedBrandId!,
      name: _nameController.text,
      description: _descriptionController.text,
      category: _categoryController.text,
      targetAudience: _targetAudienceController.text,
      sellingPoints: _sellingPointsController.text,
    );

    if (widget.productServiceId != null) {
      await ref.read(productServiceControllerProvider.notifier).updateProductService(
        widget.productServiceId!,
        request,
      );
    } else {
      await ref.read(productServiceControllerProvider.notifier).createProductService(request);
    }

    if (mounted && !ref.read(productServiceControllerProvider).hasError) {
      context.pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final brandsAsync = ref.watch(brandsProvider);
    final status = ref.watch(productServiceControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.productServiceId == null ? 'Add Product/Service' : 'Edit Product/Service'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Brand Selection
              brandsAsync.when(
                data: (brands) => DropdownButtonFormField<String>(
                  value: _selectedBrandId,
                  decoration: const InputDecoration(labelText: 'Select Brand'),
                  items: brands.map((b) => DropdownMenuItem(
                    value: b.id,
                    child: Text(b.name),
                  )).toList(),
                  onChanged: widget.productServiceId != null ? null : (val) => setState(() => _selectedBrandId = val),
                  validator: (v) => v == null ? 'Brand is required' : null,
                ),
                loading: () => const LinearProgressIndicator(),
                error: (_, __) => const Text('Error loading brands'),
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Name',
                hint: 'e.g. Premium Cotton T-shirt',
                controller: _nameController,
                validator: (v) => v == null || v.isEmpty ? 'Name is required' : null,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Category',
                hint: 'e.g. Apparel, Software, Consulting',
                controller: _categoryController,
                validator: (v) => v == null || v.isEmpty ? 'Category is required' : null,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Description',
                hint: 'Describe what this is...',
                controller: _descriptionController,
                maxLines: 3,
                validator: (v) => v == null || v.isEmpty ? 'Description is required' : null,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Target Audience',
                hint: 'Who is this for?',
                controller: _targetAudienceController,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Key Selling Points',
                hint: 'Why should they buy?',
                controller: _sellingPointsController,
                maxLines: 3,
              ),
              const SizedBox(height: 32),
              AppButton(
                text: widget.productServiceId == null ? 'Save Product' : 'Update Product',
                isLoading: status.isLoading,
                onPressed: _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
