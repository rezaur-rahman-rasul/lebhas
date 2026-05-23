import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/brands/state/brand_controller.dart';
import 'package:lebhas_creative_maker/features/product_services/state/product_service_controller.dart';
import 'package:lebhas_creative_maker/features/projects/domain/project_models.dart';
import 'package:lebhas_creative_maker/features/projects/state/project_controller.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_button.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_text_field.dart';

class ProjectFormScreen extends ConsumerStatefulWidget {
  final String? projectId;
  final String? initialProductServiceId;

  const ProjectFormScreen({
    super.key,
    this.projectId,
    this.initialProductServiceId,
  });

  @override
  ConsumerState<ProjectFormScreen> createState() => _ProjectFormScreenState();
}

class _ProjectFormScreenState extends ConsumerState<ProjectFormScreen> {
  final _formKey = GlobalKey<FormState>();
  
  String? _selectedBrandId;
  String? _selectedProductServiceId;
  late TextEditingController _nameController;
  late TextEditingController _descriptionController;
  late TextEditingController _objectiveController;
  late TextEditingController _typeController;
  String _targetPlatform = 'Facebook';

  final List<String> _platforms = ['Facebook', 'Instagram', 'TikTok', 'LinkedIn'];

  @override
  void initState() {
    super.initState();
    _selectedProductServiceId = widget.initialProductServiceId;
    _nameController = TextEditingController();
    _descriptionController = TextEditingController();
    _objectiveController = TextEditingController();
    _typeController = TextEditingController();

    if (widget.projectId != null) {
      _loadProjectData();
    }
  }

  void _loadProjectData() {
    final projectAsync = ref.read(projectDetailsProvider(widget.projectId!));
    projectAsync.whenData((project) {
      setState(() {
        _selectedBrandId = project.brandId;
        _selectedProductServiceId = project.productServiceId;
        _nameController.text = project.name;
        _descriptionController.text = project.description;
        _objectiveController.text = project.campaignObjective;
        _typeController.text = project.campaignType;
        _targetPlatform = project.targetPlatform;
      });
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    _objectiveController.dispose();
    _typeController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_selectedProductServiceId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please select a product/service')),
      );
      return;
    }

    final request = CreateProjectRequest(
      productServiceId: _selectedProductServiceId!,
      name: _nameController.text,
      description: _descriptionController.text,
      campaignObjective: _objectiveController.text,
      targetPlatform: _targetPlatform,
      campaignType: _typeController.text,
    );

    if (widget.projectId != null) {
      await ref.read(projectControllerProvider.notifier).updateProject(
        widget.projectId!,
        request,
      );
    } else {
      await ref.read(projectControllerProvider.notifier).createProject(request);
    }

    if (mounted && !ref.read(projectControllerProvider).hasError) {
      context.pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final brandsAsync = ref.watch(brandsProvider);
    final status = ref.watch(projectControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.projectId == null ? 'New Project/Campaign' : 'Edit Project'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 1. Select Brand First
              brandsAsync.when(
                data: (brands) => DropdownButtonFormField<String>(
                  value: _selectedBrandId,
                  decoration: const InputDecoration(labelText: 'Step 1: Select Brand'),
                  items: brands.map((b) => DropdownMenuItem(
                    value: b.id,
                    child: Text(b.name),
                  )).toList(),
                  onChanged: widget.projectId != null ? null : (val) {
                    setState(() {
                      _selectedBrandId = val;
                      _selectedProductServiceId = null; // Reset product selection
                    });
                  },
                  validator: (v) => v == null ? 'Brand is required' : null,
                ),
                loading: () => const LinearProgressIndicator(),
                error: (_, __) => const Text('Error loading brands'),
              ),
              const SizedBox(height: 16),

              // 2. Select Product/Service based on Brand
              if (_selectedBrandId != null)
                ref.watch(productServicesProvider(_selectedBrandId!)).when(
                  data: (products) => DropdownButtonFormField<String>(
                    value: _selectedProductServiceId,
                    decoration: const InputDecoration(labelText: 'Step 2: Select Product/Service'),
                    items: products.map((p) => DropdownMenuItem(
                      value: p.id,
                      child: Text(p.name),
                    )).toList(),
                    onChanged: widget.projectId != null ? null : (val) => setState(() => _selectedProductServiceId = val),
                    validator: (v) => v == null ? 'Product/Service is required' : null,
                  ),
                  loading: () => const LinearProgressIndicator(),
                  error: (_, __) => const Text('Error loading products'),
                ),
              
              const SizedBox(height: 16),
              AppTextField(
                label: 'Project Name',
                hint: 'e.g. Eid-ul-Adha Collection Launch',
                controller: _nameController,
                validator: (v) => v == null || v.isEmpty ? 'Project name is required' : null,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Objective',
                hint: 'e.g. Increase sales, Brand awareness',
                controller: _objectiveController,
                validator: (v) => v == null || v.isEmpty ? 'Objective is required' : null,
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                value: _targetPlatform,
                decoration: const InputDecoration(labelText: 'Target Platform'),
                items: _platforms.map((p) => DropdownMenuItem(
                  value: p,
                  child: Text(p),
                )).toList(),
                onChanged: (val) => setState(() => _targetPlatform = val!),
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Campaign Type',
                hint: 'e.g. Video Ads, Static Banners',
                controller: _typeController,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Brief Description',
                hint: 'Specific instructions for this project...',
                controller: _descriptionController,
                maxLines: 3,
              ),
              const SizedBox(height: 32),
              AppButton(
                text: widget.projectId == null ? 'Create Project' : 'Update Project',
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
