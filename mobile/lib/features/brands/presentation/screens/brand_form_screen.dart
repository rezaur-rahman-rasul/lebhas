import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/brands/domain/brand_models.dart';
import 'package:lebhas_creative_maker/features/brands/state/brand_controller.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_button.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_text_field.dart';

class BrandFormScreen extends ConsumerStatefulWidget {
  final String? brandId;
  const BrandFormScreen({super.key, this.brandId});

  @override
  ConsumerState<BrandFormScreen> createState() => _BrandFormScreenState();
}

class _BrandFormScreenState extends ConsumerState<BrandFormScreen> {
  final _formKey = GlobalKey<FormState>();
  
  late TextEditingController _nameController;
  late TextEditingController _businessTypeController;
  late TextEditingController _industryController;
  late TextEditingController _targetAudienceController;
  late TextEditingController _brandVoiceController;
  late TextEditingController _preferredCTAController;
  late TextEditingController _primaryColorController;
  late TextEditingController _secondaryColorController;
  late TextEditingController _websiteController;
  late TextEditingController _facebookController;
  late TextEditingController _instagramController;
  late TextEditingController _linkedinController;
  late TextEditingController _tiktokController;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController();
    _businessTypeController = TextEditingController();
    _industryController = TextEditingController();
    _targetAudienceController = TextEditingController();
    _brandVoiceController = TextEditingController();
    _preferredCTAController = TextEditingController();
    _primaryColorController = TextEditingController();
    _secondaryColorController = TextEditingController();
    _websiteController = TextEditingController();
    _facebookController = TextEditingController();
    _instagramController = TextEditingController();
    _linkedinController = TextEditingController();
    _tiktokController = TextEditingController();

    if (widget.brandId != null) {
      _loadBrandData();
    }
  }

  void _loadBrandData() {
    final brandAsync = ref.read(brandDetailsProvider(widget.brandId!));
    brandAsync.whenData((brand) {
      _nameController.text = brand.name;
      _businessTypeController.text = brand.businessType;
      _industryController.text = brand.industry;
      _targetAudienceController.text = brand.targetAudience;
      _brandVoiceController.text = brand.brandVoice;
      _preferredCTAController.text = brand.preferredCTA;
      _primaryColorController.text = brand.primaryColor ?? '';
      _secondaryColorController.text = brand.secondaryColor ?? '';
      _websiteController.text = brand.website ?? '';
      _facebookController.text = brand.facebookUrl ?? '';
      _instagramController.text = brand.instagramUrl ?? '';
      _linkedinController.text = brand.linkedinUrl ?? '';
      _tiktokController.text = brand.tiktokUrl ?? '';
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _businessTypeController.dispose();
    _industryController.dispose();
    _targetAudienceController.dispose();
    _brandVoiceController.dispose();
    _preferredCTAController.dispose();
    _primaryColorController.dispose();
    _secondaryColorController.dispose();
    _websiteController.dispose();
    _facebookController.dispose();
    _instagramController.dispose();
    _linkedinController.dispose();
    _tiktokController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final request = CreateBrandRequest(
      name: _nameController.text,
      businessType: _businessTypeController.text,
      industry: _industryController.text,
      targetAudience: _targetAudienceController.text,
      brandVoice: _brandVoiceController.text,
      preferredCTA: _preferredCTAController.text,
      primaryColor: _primaryColorController.text.isNotEmpty ? _primaryColorController.text : null,
      secondaryColor: _secondaryColorController.text.isNotEmpty ? _secondaryColorController.text : null,
      website: _websiteController.text.isNotEmpty ? _websiteController.text : null,
      facebookUrl: _facebookController.text.isNotEmpty ? _facebookController.text : null,
      instagramUrl: _instagramController.text.isNotEmpty ? _instagramController.text : null,
      linkedinUrl: _linkedinController.text.isNotEmpty ? _linkedinController.text : null,
      tiktokUrl: _tiktokController.text.isNotEmpty ? _tiktokController.text : null,
    );

    if (widget.brandId != null) {
      await ref.read(brandControllerProvider.notifier).updateBrand(widget.brandId!, request);
    } else {
      await ref.read(brandControllerProvider.notifier).createBrand(request);
    }

    if (mounted && !ref.read(brandControllerProvider).hasError) {
      context.pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final status = ref.watch(brandControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.brandId == null ? 'Create Brand' : 'Edit Brand'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _buildSectionHeader('Basic Information'),
              AppTextField(
                label: 'Brand Name *',
                hint: 'e.g. Lebhas Brand Attire',
                controller: _nameController,
                validator: (v) => v == null || v.isEmpty ? 'Name is required' : null,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Business Type *',
                hint: 'e.g. E-commerce, Fashion, Agency',
                controller: _businessTypeController,
                validator: (v) => v == null || v.isEmpty ? 'Required' : null,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Industry *',
                hint: 'e.g. Apparel & Fashion',
                controller: _industryController,
                validator: (v) => v == null || v.isEmpty ? 'Required' : null,
              ),
              
              const SizedBox(height: 32),
              _buildSectionHeader('Brand Identity'),
              AppTextField(
                label: 'Target Audience',
                hint: 'Describe your ideal customers',
                controller: _targetAudienceController,
                maxLines: 2,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Brand Voice',
                hint: 'e.g. Sophisticated, Playful, Professional',
                controller: _brandVoiceController,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Preferred CTA',
                hint: 'e.g. Shop Now, Get a Quote',
                controller: _preferredCTAController,
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: AppTextField(
                      label: 'Primary Color',
                      hint: '#FF0000',
                      controller: _primaryColorController,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: AppTextField(
                      label: 'Secondary Color',
                      hint: '#FFFFFF',
                      controller: _secondaryColorController,
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 32),
              _buildSectionHeader('Digital Presence'),
              AppTextField(
                label: 'Website',
                hint: 'https://',
                controller: _websiteController,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Facebook URL',
                controller: _facebookController,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Instagram URL',
                controller: _instagramController,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'LinkedIn URL',
                controller: _linkedinController,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'TikTok URL',
                controller: _tiktokController,
              ),

              const SizedBox(height: 40),
              AppButton(
                text: widget.brandId == null ? 'Create Brand Profile' : 'Save Changes',
                isLoading: status.isLoading,
                onPressed: _submit,
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Text(
        title.toUpperCase(),
        style: context.textTheme.labelSmall?.copyWith(
          fontWeight: FontWeight.bold,
          color: context.colorScheme.primary,
          letterSpacing: 1.2,
        ),
      ),
    );
  }
}
