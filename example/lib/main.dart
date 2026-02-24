import 'package:flutter/material.dart';
import 'package:freshdesk_sdk/freshdesk_sdk.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Freshdesk SDK Example',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _unreadCount = 0;
  bool _isInitialized = false;
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _initializeFreshdesk();
    _listenToUnreadCount();
  }

  Future<void> _initializeFreshdesk() async {
    try {
      await FreshdeskSDK.initialize(
        FreshdeskConfig(
          // TODO: Replace with your actual Freshdesk credentials
          token: '01KEGNWY3XZ7SYNEYXGZVBAHFV',
          host: 'https://snapnfix.freshdesk.com',
          sdkId: '01KH8Q1N0GHFFV0DM5H7ZYCNH3',
          
          // Optional: Add JWT for authenticated users
          // jwt: 'your-jwt-token-here',
          
          locale: 'en',
          debugMode: true,
        ),
      );
      
      setState(() {
        _isInitialized = true;
      });
      
      _showSnackBar('Freshdesk SDK initialized successfully!', Colors.green);
    } catch (e) {
      _showSnackBar('Failed to initialize: $e', Colors.red);
    }
  }

  void _listenToUnreadCount() {
    FreshdeskSDK.unreadCountStream.listen((count) {
      setState(() {
        _unreadCount = count;
      });
    });
  }

  Future<void> _showSupport() async {
    try {
      await FreshdeskSDK.showSupport();
    } catch (e) {
      _showSnackBar('Error: $e', Colors.red);
    }
  }

  Future<void> _setUserProperties() async {
    if (_nameController.text.isEmpty && 
        _emailController.text.isEmpty && 
        _phoneController.text.isEmpty) {
      _showSnackBar('Please fill at least one field', Colors.orange);
      return;
    }

    try {
      final properties = <String, dynamic>{};
      if (_nameController.text.isNotEmpty) {
        properties['name'] = _nameController.text;
      }
      if (_emailController.text.isNotEmpty) {
        properties['email'] = _emailController.text;
      }
      if (_phoneController.text.isNotEmpty) {
        properties['phone'] = _phoneController.text;
      }

      await FreshdeskSDK.setUserProperties(properties);
      _showSnackBar('User properties updated!', Colors.green);
      
      // Clear fields
      _nameController.clear();
      _emailController.clear();
      _phoneController.clear();
    } catch (e) {
      _showSnackBar('Error: $e', Colors.red);
    }
  }

  Future<void> _setTicketProperties() async {
    try {
      await FreshdeskSDK.setTicketProperties({
        'subject': 'App Support Request',
        'priority': 3,
        'type': 'Question',
        'status': 2,
      });
      _showSnackBar('Ticket properties set!', Colors.green);
    } catch (e) {
      _showSnackBar('Error: $e', Colors.red);
    }
  }

  Future<void> _getUnreadCount() async {
    try {
      final count = await FreshdeskSDK.getUnreadCount();
      _showSnackBar('Unread messages: $count', Colors.blue);
    } catch (e) {
      _showSnackBar('Error: $e', Colors.red);
    }
  }

  Future<void> _trackEvent() async {
    try {
      await FreshdeskSDK.trackEvent('button_clicked', {
        'button_name': 'track_event_example',
        'screen': 'home',
        'timestamp': DateTime.now().toIso8601String(),
      });
      _showSnackBar('Event tracked!', Colors.green);
    } catch (e) {
      _showSnackBar('Error: $e', Colors.red);
    }
  }

  Future<void> _clearUserData() async {
    try {
      await FreshdeskSDK.clearUserData();
      _showSnackBar('User data cleared!', Colors.green);
    } catch (e) {
      _showSnackBar('Error: $e', Colors.red);
    }
  }

  void _showSnackBar(String message, Color color) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: color,
        duration: const Duration(seconds: 2),
      ),
    );
  }

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text('Freshdesk SDK Example'),
        actions: [
          if (_unreadCount > 0)
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Stack(
                children: [
                  IconButton(
                    icon: const Icon(Icons.chat_bubble_outline),
                    onPressed: _showSupport,
                  ),
                  Positioned(
                    right: 0,
                    top: 0,
                    child: Container(
                      padding: const EdgeInsets.all(4),
                      decoration: const BoxDecoration(
                        color: Colors.red,
                        shape: BoxShape.circle,
                      ),
                      child: Text(
                        '$_unreadCount',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
      body: _isInitialized
          ? SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Status Card
                  Card(
                    color: Colors.green.shade50,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          const Icon(Icons.check_circle, color: Colors.green),
                          const SizedBox(width: 12),
                          const Expanded(
                            child: Text(
                              'SDK Initialized',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 6,
                            ),
                            decoration: BoxDecoration(
                              color: _unreadCount > 0 
                                  ? Colors.red 
                                  : Colors.grey,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Text(
                              'Unread: $_unreadCount',
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),

                  // User Properties Section
                  const Text(
                    'Set User Properties',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _nameController,
                    decoration: const InputDecoration(
                      labelText: 'Name',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.person),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _emailController,
                    decoration: const InputDecoration(
                      labelText: 'Email',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.email),
                    ),
                    keyboardType: TextInputType.emailAddress,
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _phoneController,
                    decoration: const InputDecoration(
                      labelText: 'Phone',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.phone),
                    ),
                    keyboardType: TextInputType.phone,
                  ),
                  const SizedBox(height: 12),
                  ElevatedButton.icon(
                    onPressed: _setUserProperties,
                    icon: const Icon(Icons.save),
                    label: const Text('Update User Properties'),
                  ),
                  const SizedBox(height: 24),

                  // Action Buttons
                  const Text(
                    'Actions',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 12),
                  ElevatedButton.icon(
                    onPressed: _showSupport,
                    icon: const Icon(Icons.chat),
                    label: const Text('Show Support Chat'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.blue,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.all(16),
                    ),
                  ),
                  const SizedBox(height: 12),
                  ElevatedButton.icon(
                    onPressed: _setTicketProperties,
                    icon: const Icon(Icons.confirmation_number),
                    label: const Text('Set Ticket Properties'),
                  ),
                  const SizedBox(height: 12),
                  ElevatedButton.icon(
                    onPressed: _getUnreadCount,
                    icon: const Icon(Icons.notifications),
                    label: const Text('Get Unread Count'),
                  ),
                  const SizedBox(height: 12),
                  ElevatedButton.icon(
                    onPressed: _trackEvent,
                    icon: const Icon(Icons.analytics),
                    label: const Text('Track Custom Event'),
                  ),
                  const SizedBox(height: 12),
                  ElevatedButton.icon(
                    onPressed: _clearUserData,
                    icon: const Icon(Icons.logout),
                    label: const Text('Clear User Data (Logout)'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ],
              ),
            )
          : const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 16),
                  Text('Initializing Freshdesk SDK...'),
                ],
              ),
            ),
      floatingActionButton: _isInitialized
          ? FloatingActionButton.extended(
              onPressed: _showSupport,
              icon: Stack(
                children: [
                  const Icon(Icons.support_agent),
                  if (_unreadCount > 0)
                    Positioned(
                      right: 0,
                      top: 0,
                      child: Container(
                        padding: const EdgeInsets.all(4),
                        decoration: const BoxDecoration(
                          color: Colors.red,
                          shape: BoxShape.circle,
                        ),
                        constraints: const BoxConstraints(
                          minWidth: 16,
                          minHeight: 16,
                        ),
                      ),
                    ),
                ],
              ),
              label: const Text('Get Support'),
            )
          : null,
    );
  }
}
